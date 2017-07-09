package com.example.fanquan.tcptest;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    OutputStream _outputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Log.i("FQ", "clicked");

                try {
                    _outputStream.write("From Android\n".getBytes());
                    _outputStream.flush();
                } catch (IOException e) {
                    Log.e("FQ", "E at output1: " + e.getMessage());
                }
            }
        });

        new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("192.168.3.233", 23333);
                    OutputStream outputStream = socket.getOutputStream();
                    _outputStream = outputStream;
                    outputStream.write("From Android\n".getBytes());
                    outputStream.flush();

                    InputStream inputStream = socket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    int n = 1000;
                    while (n-- > 0) {
                        String data = bufferedReader.readLine();

                        Log.i("FQ", "got replied from server: " + data);
                    }

                    // close
                    bufferedReader.close();
                    inputStream.close();
                    outputStream.close();
                    socket.close();
                } catch (Exception e) {
                    Log.e("FQ", "E occured: " + e.getMessage());
                }
            }
        }.start();
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
