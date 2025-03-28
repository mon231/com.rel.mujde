package com.rel.mujde.api;
import com.rel.mujde.api.model.Script;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ScriptServer {
    @GET("scripts")
    Call<List<Script>> getAllScripts();

    @POST("scripts")
    Call<Script> uploadScript(@Body Script script);

    @GET("scripts/{id}")
    Call<Script> getScriptById(@Path("id") int id);

    @DELETE("scripts/{id}")
    Call<Void> deleteScript(@Path("id") int id);
}
