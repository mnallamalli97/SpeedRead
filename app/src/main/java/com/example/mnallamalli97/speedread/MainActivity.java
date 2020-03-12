package com.example.mnallamalli97.speedread;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import static android.os.Environment.DIRECTORY_DOWNLOADS;


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
    private String bookPath;

    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance("gs://speedread1214.appspot.com/");
    StorageReference storageReference;
    StorageReference ref;

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
        bookPath = extras.getString("book_path", "BOOKPATH");
        wordSpeedTextView.setText(String.valueOf(newSpeed));
        bookTitleTextView.setText(String.valueOf(book));

        //i want 400 words per min.
        //currently every 400 ms, the word switches.
        runWords(bookPath, newSpeed);

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


    public void runWords(final String bookPath, final long speed) {


        //DOWNLOADING BOOK ONTO DEVICE
        // Instantiates a client
        storageReference = firebaseStorage.getReference();
        ref = storageReference.child("books/" + bookPath);


        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                String url = uri.toString();
                downloadFileAndShowWords(MainActivity.this, bookPath, ".txt", DIRECTORY_DOWNLOADS, url, speed);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }



    public void downloadFileAndShowWords(Context context, String fileName, String fileExtension, String destinationDirectory, String url, final long speed){

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, destinationDirectory, fileName + fileExtension);

        downloadManager.enqueue(request);



        // Local temp file has been created
        //load file into input stream and split each word to display in textview
        String line;

        File path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        String downloadPath = path.getAbsolutePath() + "/" + bookPath + ".txt";


        try (


               // InputStream fis = getApplicationContext().getFilesDir().get
              //  InputStream fis = getApplicationContext().getAssets().open("req.txt");
               InputStream is = new FileInputStream(downloadPath);
                InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
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
}
