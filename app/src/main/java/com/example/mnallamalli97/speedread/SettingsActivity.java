package com.example.mnallamalli97.speedread;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private long seekBarValue;
    private SeekBar seekBar;
    private Button saveButton;
    private Button libraryButton;
    private TextView resultText;
    private TextView bookTitle;
    private TextView bookAuthor;
    private TextView bookPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        final SharedPreferences.Editor editor = pref.edit();

        Bundle extras = getIntent().getExtras();

        saveButton = findViewById(R.id.saveButton);
        libraryButton = findViewById(R.id.libraryButton);
        seekBar = findViewById(R.id.seekBar);
        resultText =findViewById(R.id.resultText);
        bookAuthor =findViewById(R.id.bookAuthor);
        bookTitle =findViewById(R.id.bookTitle);
        bookPath = findViewById(R.id.bookPath);

        final String author = String.valueOf(extras.getString("author"));
        final String title = String.valueOf(extras.getString("title"));
        final String path = String.valueOf(extras.getString("book_path"));

        bookAuthor.setText(author);
        bookTitle.setText(title);
        bookPath.setText(path);

        seekBar.setProgress((int) pref.getLong("speedReadSpeed", 250));
        String speedString = String.valueOf(pref.getLong("speedReadSpeed", 250));
        resultText.setText(speedString);
        seekBar.setMax(800);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int yourStep = 50;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarValue = progress;

                progress = ((int)Math.round(progress/yourStep ))*yourStep;
                seekBar.setProgress(progress);
                resultText.setText(progress + "");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                resultText.setText(seekBarValue + "");

                editor.putLong("speedReadSpeed",seekBarValue);
                editor.apply();
            }
        });


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(SettingsActivity.this, MainActivity.class);
                Bundle extras = new Bundle();
                extras.putLong("speedreadSpeed", seekBarValue);
                extras.putString("title", title );
                extras.putString("author", author );
                extras.putString("book_path", path );

                startIntent.putExtras(extras);
                startActivity(startIntent);
            }
        });

        libraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(SettingsActivity.this, LibraryActivity.class);
                startActivity(startIntent);
            }
        });

    }
}
