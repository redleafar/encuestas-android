package com.rafaelbermudez.encuestas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class loginActivity extends AppCompatActivity {

    TextInputEditText email;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private String emailString;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);

        sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
        editor = sharedPref.edit();

        emailString = sharedPref.getString(getString(R.string.email),"DEFAULT");

        if (!"DEFAULT".equals(emailString)){
            email.setText(emailString);
        }

        button = findViewById(R.id.button);

        final Intent intent = new Intent(this, MainActivity.class);

        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                if ((email.getText().toString().trim().length() != 0) && (android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches())){
                    editor.putString(getString(R.string.email), email.getText().toString().trim());
                    editor.putBoolean("session_started", true);
                    editor.apply();
                    startActivity(intent);
                    finish();
                }
                else{
                    Toast.makeText(loginActivity.this, "Ingrese un email válido", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}
