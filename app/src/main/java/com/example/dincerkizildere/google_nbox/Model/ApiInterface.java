package com.example.dincerkizildere.google_nbox.Model;

import com.example.dincerkizildere.google_nbox.Common.Const.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiInterface {
    @GET(Api.Inbox)
    Call<List<Message>> getInbox();
}