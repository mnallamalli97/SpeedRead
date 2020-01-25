package com.example.mnallamalli97.speedread;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Button settingsButton;
    private Button startButton;
    private Button pauseButton;
    private TextView wordTextView;
    final Timer utilTimer = new Timer();
    int pauseFlag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsButton = findViewById(R.id.settingsButton);
        wordTextView = findViewById(R.id.mainWord);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);


        /*
            60 seconds a minute.
            60/3 seconds = 20 words a minute
        */

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("start button pressed");
                runWords(3000L);
                System.out.println("runWords() function finished");
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("pause button pressed");
                pauseFlag = 1;
            }
        });

    }



    public void runWords(long speed) {
        final String[] random_words = {"this", "is", "a", "test"};


        utilTimer.scheduleAtFixedRate(new TimerTask() {
            private int index = 0;

            public void run() {
                System.out.println(random_words[index]);
                wordTextView.post(new Runnable() {
                    public void run() {
                        wordTextView.setText(random_words[index]);
                    }
                });
                if(pauseFlag == 0){
                    index++;
                }

                if (index >= random_words.length) {
                    utilTimer.cancel();
                }
            }
        }, 3000L, speed);
    }
}
