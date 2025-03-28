package com.rel.mujde.api;

import com.rel.mujde.Constants;
import android.content.Context;
import android.content.SharedPreferences;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    public static ScriptServer getClient(Context context) {
        if (retrofit == null) {
            SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
            String repository = prefs.getString(Constants.PREF_SCRIPTS_REPOSITORY, Constants.DEFAULT_REPOSITORY);

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://" + repository + "/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit.create(ScriptServer.class);
    }
}
