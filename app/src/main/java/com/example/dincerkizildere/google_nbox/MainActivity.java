package com.example.dincerkizildere.google_nbox;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.dincerkizildere.google_nbox.Adapter.MessagesAdapter;
import com.example.dincerkizildere.google_nbox.Common.Helper.DividerItemDecoration;
import com.example.dincerkizildere.google_nbox.Common.Utils.UtilsApi;
import com.example.dincerkizildere.google_nbox.Model.ApiInterface;
import com.example.dincerkizildere.google_nbox.Model.Message;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, MessagesAdapter.MessageAdapterListener {


    private List<Message> messages=new ArrayList<>();
    private RecyclerView recyclerView;
    private MessagesAdapter mAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ActionModeCallback actionModeCallBack;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab=(FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        mAdapter = new MessagesAdapter(this, messages, this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        actionModeCallBack =new ActionModeCallback();

        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        getInbox();
                    }
                }
        );
    }

    private void getInbox() {
        swipeRefreshLayout.setRefreshing(true);

        ApiInterface apiService=UtilsApi.getClient().create(ApiInterface.class);

        Call<List<Message>> call=apiService.getInbox();
        call.enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                messages.clear();

                for (Message message : response.body()) {
                    // generate a random color
                    message.setColor(getRandomMaterialColor("400"));
                    messages.add(message);
                }

                mAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(getApplicationContext(),"Unable to fetch json: " + t.getMessage(), Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);

            }
        });
    }

    private int getRandomMaterialColor(String typeColor) {
        int returnColor=Color.GRAY;
        int arrayId=getResources().getIdentifier("mdcolor_" + typeColor, "array", getPackageName());

        if (arrayId!=0){
            TypedArray colors=getResources().obtainTypedArray(arrayId);
            int index=(int) (Math.random()* colors.length());
            returnColor=colors.getColor(index, Color.GRAY);
            colors.recycle();
        }
        return returnColor;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        int id= item.getItemId();

        if (id==R.id.action_search){
            Toast.makeText(getApplicationContext(),"Search...", Toast.LENGTH_SHORT).show();
            return true;
        }

        return  super.onOptionsItemSelected(item);
    }


    @Override
    public void onRefresh() {
        getInbox();
    }

    @Override
    public void onIconClicked(int position) {
        if (actionMode==null){
            actionMode=startSupportActionMode(actionModeCallBack);
        }

        toggleSelection(position);

    }

    @Override
    public void onIconImportantClicked(int position) {

        Message message=messages.get(position);
        message.setImportant(!message.isImportant());
        messages.set(position,message);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    public void onMessageRowClicked(int position) {

        if (mAdapter.getSelectedItemCount()>    0){
            enableActionMode(position);
        }else{
            Message message=messages.get(position);
            message.setRead(true);
            messages.set(position,message);
            mAdapter.notifyDataSetChanged();

            Toast.makeText(getApplicationContext(), " Read:" + message.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRowLongClicked(int position) {

        enableActionMode(position);


    }

    private void enableActionMode(int position) {
        if (actionMode==null){
            actionMode=startSupportActionMode(actionModeCallBack);
        }

        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count=mAdapter.getSelectedItemCount();

        if (count==0){
            actionMode.finish();
        }
        else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private class ActionModeCallback implements ActionMode.Callback{

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);

            swipeRefreshLayout.setEnabled(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    deleteMessages();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            swipeRefreshLayout.setEnabled(true);
            actionMode=null;
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.resetAnimationIndex();
                }
            });

        }
    }

    private void deleteMessages(){
        mAdapter.resetAnimationIndex();
        List<Integer> selectedItemPositions=
                mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size()-1; i>=0 ; i--) {
            mAdapter.removeData(selectedItemPositions.get(i));


        }
        mAdapter.notifyDataSetChanged();
    }
}
