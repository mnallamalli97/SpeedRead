package com.example.mnallamalli97.speedread;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private Button settingsButton;
    private Button startButton;
    private Button pauseButton;
    private TextView wordTextView;
    private TextView wordSpeedTextView;
    final Timer utilTimer = new Timer();
    boolean cancelled = false;
    private long newSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsButton = findViewById(R.id.settingsButton);
        wordTextView = findViewById(R.id.mainWord);
        wordSpeedTextView = findViewById(R.id.wordSpeed);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);


        /*
            60 seconds a minute.
            60/3 seconds = 20 words a minute
        */

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        newSpeed = extras.getLong("speedreadSpeed", 10000L);

        wordSpeedTextView.setText(String.valueOf(newSpeed));
        runWords(newSpeed);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelled = false;
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelled = true;
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(startIntent);
            }
        });

    }


    public void runWords(final long speed) {

        String line;

        try (

                InputStream fis = getApplicationContext().getAssets().open("req.txt");
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        ) {
            while ((line = br.readLine()) != null) {

                final String[] wc = line.split(" ");

                    final android.os.Handler handler = new android.os.Handler();
                    handler.post(new Runnable() {

                        int i = 0;

                        @Override
                        public void run() {
                            System.out.println(wc[i]);
                            wordTextView.setText(wc[i]);
                            i++;
                            if (i == wc.length) {
                                handler.removeCallbacks(this);
                            } else {
                                if(cancelled == false)
                                    handler.postDelayed(this, speed);
                            }
                        }
                    });
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
