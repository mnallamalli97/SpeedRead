package com.example.mnallamalli97.speedread;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar seekBar = findViewById(R.id.seekBar);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
