package com.rel.mujde.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.rel.mujde.Constants;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Get repository URL from SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
            String repository = prefs.getString(Constants.PREF_SCRIPTS_REPOSITORY, Constants.DEFAULT_REPOSITORY);
            
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://" + repository + "/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
    
    // Reset the client when repository changes
    public static void resetClient() {
        retrofit = null;
    }
}
