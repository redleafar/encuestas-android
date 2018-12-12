package com.rafaelbermudez.encuestas.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.rafaelbermudez.encuestas.Entities.Poll;
import com.rafaelbermudez.encuestas.Entities.SQLiteHelperConnection;
import com.rafaelbermudez.encuestas.MainActivity;
import com.rafaelbermudez.encuestas.R;
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

public class SendingService extends Service {

    static final String CONNECTIVITY_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    private ArrayList<Poll> mPollsList = new ArrayList<Poll>();
    private SQLiteHelperConnection mConn;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private Boolean update;
    private Boolean flag = false;

    private String mEmail;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            String channelId = "default_channel_id2";
            String channelDescription = "Default Channel";

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            NotificationChannel notificationChannel = mNotificationManager.getNotificationChannel(channelId);
            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_LOW; //Set the importance level
                notificationChannel = new NotificationChannel(channelId, channelDescription, importance);
                notificationChannel.setVibrationPattern(new long[]{ 0 });
                notificationChannel.enableVibration(true);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }

            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Encuestas")
                    .setContentText("El servicio de encuestas está activo")
                    .setChannelId(channelId);

            Notification notification = builder.build();

            startForeground(123, notification);

        }
        // Let it continue running until it is stopped.
        //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (CONNECTIVITY_CHANGE_ACTION.equals(action)) {
                    //check internet connection
                    if (!ConnectionHelper.isConnectedOrConnecting(context)) {
                        if (context != null) {
                            boolean show = false;
                            if (ConnectionHelper.lastNoConnectionTs == -1) {//first time
                                show = true;
                                ConnectionHelper.lastNoConnectionTs = System.currentTimeMillis();
                            } else {
                                if (System.currentTimeMillis() - ConnectionHelper.lastNoConnectionTs > 1000) {
                                    show = true;
                                    ConnectionHelper.lastNoConnectionTs = System.currentTimeMillis();
                                }
                            }

                            if (show && ConnectionHelper.isOnline) {
                                ConnectionHelper.isOnline = false;
                                Log.i("NETWORK123","Connection lost");
                                //manager.cancelAll();
                            }
                        }
                    } else {
                        Log.i("NETWORK123","Connected");

                        ConnectionHelper.isOnline = true;

                        sharedPref = context.getSharedPreferences(
                                getString(R.string.preferences), Context.MODE_PRIVATE);

                        editor = sharedPref.edit();

                        update = sharedPref.getBoolean(getString(R.string.update),true);
                        mEmail = sharedPref.getString(getString(R.string.email),"DEFAULT");

                        if (!flag){
                            getPollList();
                        }

                        //Toast.makeText(context, update.toString(), Toast.LENGTH_LONG).show();

                        if (update){
                            if (mPollsList.size() > 0){
                                if (!flag) {
                                    flag = true;
                                    new UploadPolls().execute(getString(R.string.api_domain) + "/mobile/uploadpolls");
                                }
                            }
                        }
                    }
                }
            }
        };
        registerReceiver(receiver,filter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }
    }

    private class UploadPolls extends AsyncTask<String, Void, String> {

        public void onPreExecute() {

        }

        public UploadPolls() {

        }

        @Override
        protected String doInBackground(String... params) {
            //do your request in here so that you don't interrupt the UI thread
            //Toast.makeText(ResultsActivity.this, "do in background", Toast.LENGTH_LONG).show();
            try {
                return uploadPollsRequest(params[0]);
            } catch (IOException e) {
                //flag = false;
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

                    Intent intent = new Intent();
                    intent.setAction("com.rafaelbermudez.encuestas");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
                else{
                    editor.putBoolean(getString(R.string.update), true);
                    editor.apply();
                }

                //Toast.makeText(MainActivity.this, status , Toast.LENGTH_LONG).show();
                Log.d("request status", status);

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                editor.putBoolean(getString(R.string.update), true);
                editor.apply();
                //Toast.makeText(MainActivity.this, "Se presentó un error: "+e.toString(), Toast.LENGTH_SHORT).show();

            }

            flag = false;
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