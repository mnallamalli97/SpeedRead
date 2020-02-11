package com.example.mnallamalli97.speedread;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Timer;



public class MainActivity extends AppCompatActivity {

    private Button settingsButton;
    private Button startButton;
    private Button pauseButton;
    private Button libraryButton;
    private TextView wordTextView;
    private TextView wordSpeedTextView;
    private TextView bookTitleTextView;
    boolean cancelled = false;
    private long newSpeed = 0;
    private String book;
    File localFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsButton = findViewById(R.id.settingsButton);
        libraryButton = findViewById(R.id.libraryButton);
        wordTextView = findViewById(R.id.mainWord);
        wordSpeedTextView = findViewById(R.id.wordSpeed);
        bookTitleTextView = findViewById(R.id.bookTitle);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        Bundle extras = getIntent().getExtras();


        /*
            1000 ms in a second
            60 seconds a minute.

            speed should be: show a word every 250 ms.
        */




        newSpeed = extras.getLong("speedreadSpeed", 250);
        book = extras.getString("title", "BOOKTITLE");
        wordSpeedTextView.setText(String.valueOf(newSpeed));
        bookTitleTextView.setText(String.valueOf(book));

        //i want 400 words per min.
        //currently every 400 ms, the word switches.
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

        libraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(MainActivity.this, LibraryActivity.class);
                startActivity(startIntent);
            }
        });

    }


    public void runWords(final long speed) {


        //DOWNLOADING BOOK ONTO DEVICE
        // Instantiates a client
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference = storage.getReferenceFromUrl("gs://speedread1214.appspot.com/books/" + "req.txt");


        try {
            localFile = File.createTempFile("book", "txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        gsReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                //load file into input stream and split each word to display in textview
                String line;

                try (
                        InputStream fis = getApplicationContext().getAssets().open(localFile.getAbsolutePath());
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
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });


        System.out.printf("Bucket %s created.%n", localFile.getName());


    }
}
