package com.rafaelbermudez.encuestas;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.rafaelbermudez.encuestas.Entities.Poll;
import com.rafaelbermudez.encuestas.Entities.SQLiteHelperConnection;
import com.rafaelbermudez.encuestas.Services.SendingService;
import com.rafaelbermudez.encuestas.Utilities.ConnectionHelper;
import com.rafaelbermudez.encuestas.Utilities.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PollActivity extends AppCompatActivity {

    private TextInputEditText mFirstname;
    private TextInputEditText mLastname;
    private TextInputEditText mAge;
    private Spinner mAnswer1;
    private Spinner mAnswer2;
    private Spinner mAnswer3;
    private Button mSavePoll;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private ArrayList<Poll> mPollsList = new ArrayList<Poll>();
    private SQLiteHelperConnection mConn;

    private String mEmail;

    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);
        setTitle(R.string.new_poll);

        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setMessage("Subiendo encuesta...");

        mFirstname = findViewById(R.id.poll_firstname);
        mLastname = findViewById(R.id.poll_lastname);
        mAge = findViewById(R.id.poll_age);
        mAnswer1 = findViewById(R.id.poll_answer1);
        mAnswer2 = findViewById(R.id.poll_answer2);
        mAnswer3 = findViewById(R.id.poll_answer3);
        mSavePoll = findViewById(R.id.save_poll_button);

        mSavePoll.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mFirstname.getText().length() > 0){
                    if (mLastname.getText().length() > 0){
                        if (mAge.getText().length() > 0){
                            if (mAnswer1.getSelectedItemPosition() != 0){
                                if (mAnswer2.getSelectedItemPosition() != 0){
                                    if (mAnswer3.getSelectedItemPosition() != 0){
                                        registerPoll();
                                    }
                                    else{
                                        Toast.makeText(PollActivity.this, "Seleccione qué le gusta más de esta región", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else{
                                    Toast.makeText(PollActivity.this, "Seleccione si viaja o no por primera vez a esta región", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else{
                                Toast.makeText(PollActivity.this, "Seleccione un motivo de viaje", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else{
                            Toast.makeText(PollActivity.this, "Ingrese su edad", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(PollActivity.this, "Ingrese su apellido", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(PollActivity.this, "Ingrese su nombre", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void registerPoll(){
        SQLiteHelperConnection conn = new SQLiteHelperConnection(this, "db_polls", null, 1);

        SQLiteDatabase db = conn.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Utilities.UPLOADED_FIELD, 0);
        values.put(Utilities.FIRSTNAME_FIELD, mFirstname.getText().toString());
        values.put(Utilities.LASTNAME_FIELD, mLastname.getText().toString());
        values.put(Utilities.AGE_FIELD, mAge.getText().toString());
        values.put(Utilities.ANSWER1_FIELD, mAnswer1.getSelectedItem().toString());
        values.put(Utilities.ANSWER2_FIELD, mAnswer2.getSelectedItem().toString());
        values.put(Utilities.ANSWER3_FIELD, mAnswer3.getSelectedItem().toString());

        Long id = db.insert(Utilities.POLL_TABLE, Utilities.ID_FIELD, values);
        db.close();

        //Toast.makeText(this, "Insertada encuesta con el id " + id, Toast.LENGTH_SHORT).show();

        sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
        editor = sharedPref.edit();

        mEmail = sharedPref.getString(getString(R.string.email),"DEFAULT");

        if (ConnectionHelper.isConnectedOrConnecting(this)){
            new UploadPolls(progress).execute(getString(R.string.api_domain)+"/mobile/uploadpolls");
        }
        else{
            editor.putBoolean(getString(R.string.update), true);
            editor.apply();

            Toast.makeText(PollActivity.this, "Las encuestas se subirán cuando haya conexión a internet", Toast.LENGTH_SHORT).show();
            finish();
        }


    }

    private class UploadPolls extends AsyncTask<String, Void, String> {

        ProgressDialog progress;

        public void onPreExecute() {
            progress.show();
            getPollList();
        }

        public UploadPolls(ProgressDialog progress) {
            this.progress = progress;
        }

        @Override
        protected String doInBackground(String... params) {
            //do your request in here so that you don't interrupt the UI thread
            //Toast.makeText(ResultsActivity.this, "do in background", Toast.LENGTH_LONG).show();
            try {
                return uploadPollsRequest(params[0]);
            } catch (IOException e) {
                editor.putBoolean(getString(R.string.update), true);
                editor.apply();
                return "Error al conectar al servidor.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);

                String status = jsonObject.optString("status");

                if ("success".equals(status)){
                    editor.putBoolean(getString(R.string.update), false);
                    editor.apply();

                    updatePolls();

                    Toast.makeText(PollActivity.this, "Se han subido las encuestas", Toast.LENGTH_SHORT).show();
                }
                else{
                    editor.putBoolean(getString(R.string.update), true);
                    editor.apply();

                    Toast.makeText(PollActivity.this, "Las encuestas se subirán cuando haya conexión a internet", Toast.LENGTH_SHORT).show();
                }

                //Toast.makeText(MainActivity.this, status , Toast.LENGTH_LONG).show();
                Log.d("request status", status);

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                editor.putBoolean(getString(R.string.update), true);
                editor.apply();
                Toast.makeText(PollActivity.this, "Las encuestas se subirán cuando haya conexión a internet", Toast.LENGTH_SHORT).show();
                //Toast.makeText(MainActivity.this, "Se presentó un error: "+e.toString(), Toast.LENGTH_SHORT).show();

            }

            finish();
            progress.hide();
        }
    }

    private String uploadPollsRequest(String myurl) throws IOException {
        //Toast.makeText(ResultsActivity.this, "download content:fgfg", Toast.LENGTH_LONG).show();
        InputStream is = null;
        int length = 5000;


        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(50000 /* milliseconds */);
            conn.setConnectTimeout(65000 /* milliseconds */);


            /* Para hacer post sería con esto si es con get ignorar esas líneas (ojop que conn.setRequestMethod("GET") cambiaría por POST)*/
            conn.setRequestMethod("POST");
            conn.setDoInput(true);

            conn.setDoOutput(true);

            Uri.Builder builder = new Uri.Builder();

            for (int i=0;i < mPollsList.size();i++){
                builder.appendQueryParameter("polls["+i+"][0]", mPollsList.get(i).getFirstname());
                builder.appendQueryParameter("polls["+i+"][1]", mPollsList.get(i).getLastname());
                builder.appendQueryParameter("polls["+i+"][2]", String.valueOf(mPollsList.get(i).getAge()));
                builder.appendQueryParameter("polls["+i+"][3]", mPollsList.get(i).getAnswer1());
                builder.appendQueryParameter("polls["+i+"][4]", String.valueOf(mPollsList.get(i).getUploaded()));
                builder.appendQueryParameter("polls["+i+"][5]", String.valueOf(mPollsList.get(i).getAnswer2()));
                builder.appendQueryParameter("polls["+i+"][6]", String.valueOf(mPollsList.get(i).getAnswer3()));
            }

            builder.appendQueryParameter("email", mEmail);

            String query = builder.build().getEncodedQuery();

            //Toast.makeText(ResultsActivity.this, "downloadcontent", Toast.LENGTH_LONG).show();
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();
            /* Para hacer post sería con esto con get ignorar esas lineas */

            conn.connect();
            int response = conn.getResponseCode();

            is = conn.getInputStream();
            //Toast.makeText(CreateRouteActivity.this, "The response is: " + response, Toast.LENGTH_LONG).show();
            // Convert the InputStream into a string
            String contentAsString = convertInputStreamToString(is, length);
            return contentAsString;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String convertInputStreamToString(InputStream stream, int length) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        BufferedReader r = new BufferedReader(reader);
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }

        return total.toString();
    }

    private void getPollList(){
        mConn = new SQLiteHelperConnection(this, "db_polls", null, 1);
        SQLiteDatabase db = mConn.getReadableDatabase();

        mPollsList.clear();

        Cursor cursor = db.rawQuery("SELECT * FROM "+Utilities.POLL_TABLE, null);

        while(cursor.moveToNext()){
            Poll poll = new Poll(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getString(5), cursor.getString(6), cursor.getString(7));
            mPollsList.add(poll);
        }
    }

    private void updatePolls(){
        mConn = new SQLiteHelperConnection(this, "db_polls", null, 1);
        SQLiteDatabase db = mConn.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM "+Utilities.POLL_TABLE, null);

        while(cursor.moveToNext()){
            ContentValues values = new ContentValues();
            values.put(Utilities.UPLOADED_FIELD, 1);

            String[] parameters = {String.valueOf(cursor.getInt(0))};

            db.update(Utilities.POLL_TABLE, values, Utilities.ID_FIELD + "=?", parameters);
        }
        db.close();
    }
}
