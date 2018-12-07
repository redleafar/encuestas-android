package com.rafaelbermudez.encuestas;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.rafaelbermudez.encuestas.Adapters.PollsAdapter;
import com.rafaelbermudez.encuestas.Entities.Poll;
import com.rafaelbermudez.encuestas.Entities.SQLiteHelperConnection;
import com.rafaelbermudez.encuestas.Services.SendingService;
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

public class MainActivity extends AppCompatActivity {

    ListView mPollsListView;
    ArrayList<Poll> mPollsList = new ArrayList<Poll>();
    PollsAdapter mPollsAdapter;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    //Button mUploadButton;

    SQLiteHelperConnection mConn;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setMessage("Subiendo encuestas...");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
        editor = sharedPref.edit();


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PollActivity.class);
                startActivity(intent);
            }
        });

        setTitle(R.string.polls_list);

        mPollsListView = findViewById(R.id.polls_list_view);
        //mUploadButton = findViewById(R.id.upload_button);
        mPollsAdapter = new PollsAdapter(this, R.layout.poll_view, mPollsList);
        mPollsListView.setAdapter(mPollsAdapter);
        mConn = new SQLiteHelperConnection(this, "db_polls", null, 1);

        getPollList();

//        mUploadButton.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new UploadPolls(progress, MainActivity.this).execute(getString(R.string.api_domain)+"/mobile/uploadpolls");
//            }
//        });
        IntentFilter intent= new IntentFilter("com.rafaelbermudez.encuestas");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onMessage, intent);

        ContextCompat.startForegroundService(MainActivity.this, new Intent(getBaseContext(), SendingService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            final SQLiteDatabase db = mConn.getReadableDatabase();

            new AlertDialog.Builder(this)
                    .setTitle("Salir de la aplicación")
                    .setMessage("¿Desea salir?")
                    .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            editor.putBoolean("session_started", false);
                            editor.apply();

                            finish();
                        }
                    }).setNegativeButton("No", null).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPollsList.clear();
        getPollList();
    }

    private void getPollList(){
        SQLiteDatabase db = mConn.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM "+Utilities.POLL_TABLE, null);

        while(cursor.moveToNext()){
            Poll poll = new Poll(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getString(5), cursor.getString(6), cursor.getString(7));
            mPollsList.add(poll);
        }

        mPollsAdapter.notifyDataSetChanged();
    }

    private BroadcastReceiver onMessage= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MainActivity.this, "Encuestas subidas al servidor", Toast.LENGTH_SHORT).show();

            mPollsList.clear();
            getPollList();
        }
    };
}
