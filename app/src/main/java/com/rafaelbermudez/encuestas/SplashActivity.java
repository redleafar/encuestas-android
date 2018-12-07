package com.rafaelbermudez.encuestas;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent;

        sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
        Boolean sessionStartedValue = sharedPref.getBoolean("session_started",false);

        if (sessionStartedValue){
            intent = new Intent(this, MainActivity.class);

        }
        else{
            intent = new Intent(this, loginActivity.class);
        }


        startActivity(intent);
        finish();
    }
}