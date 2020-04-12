package com.example.mnallamalli97.speedread;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.mnallamalli97.speedread.news.NewsActivity;
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

    private Button pauseButton;
    private Button importButton;
    private Button newsButton;
    private Button rewindButton;
    private Button forwardButton;
    private TextView wordTextView;
    private TextView wordSpeedTextView;
    private TextView bookTitleTextView;
    private ProgressBar bookProgress;
    private boolean isPlaying = true;
    private long newSpeed = 0;
    private String bookTitle;
    private String bookAuthor;
    private String bookPath;
    private boolean isDark;
    private int wordCount = 0;
    private String mUrl, mImg, mTitle, mDate, mSource, mAuthor, mContent;

    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance("gs://speedread1214.appspot.com/");
    StorageReference storageReference;
    StorageReference ref;

    // Request code for selecting a PDF document.
    private static final int PICK_PDF_FILE = 1214;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set default to light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        wordTextView = findViewById(R.id.mainWord);
        wordSpeedTextView = findViewById(R.id.wordSpeed);
        bookTitleTextView = findViewById(R.id.bookTitle);
        pauseButton = findViewById(R.id.pauseButton);
        importButton = findViewById(R.id.importButton);
        newsButton = findViewById(R.id.newsButton);
        forwardButton = findViewById(R.id.forwardButton);
        rewindButton = findViewById(R.id.rewindButton);
        bookProgress = findViewById(R.id.bookProgress);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        final SharedPreferences.Editor editor = pref.edit();
        final Bundle extras = getIntent().getExtras();

        newSpeed = extras.getLong("speedreadSpeed", 250);
        bookTitle = extras.getString("title", "BOOKTITLE");
        bookAuthor = extras.getString("author", "BOOKAUTHOR");
        bookPath = extras.getString("book_path", "BOOKPATH");
        isDark = extras.getBoolean("darkModeEnabled");
        wordSpeedTextView.setText(String.valueOf(newSpeed));
        bookTitleTextView.setText(String.valueOf(bookTitle));

        Intent intent = getIntent();
        mUrl = intent.getStringExtra("url");
        mImg = intent.getStringExtra("img");
        mTitle = intent.getStringExtra("title");
        mDate = intent.getStringExtra("date");
        mSource = intent.getStringExtra("source");
        mAuthor = intent.getStringExtra("author");
        mContent = intent.getStringExtra("content");



        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_settings:
                        Intent startSettingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        Bundle extras = new Bundle();
                        extras.putLong("speedreadSpeed", newSpeed);
                        extras.putString("title", bookTitle );
                        extras.putString("author", bookAuthor );
                        extras.putString("book_path", bookPath );
                        extras.putBoolean("darkModeEnabled", isDark );
                        startSettingsIntent.putExtras(extras);
                        startActivity(startSettingsIntent);
                        break;
                    case R.id.action_library:
                        Intent startLibraryIntent = new Intent(MainActivity.this, LibraryActivity.class);
                        startActivity(startLibraryIntent);
                        break;
                }
                return true;
            }
        });

//        if()
            runLibraryWords(bookPath, newSpeed);
//        else
//            runNewsWords(newSpeed);

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    pauseButton.setBackgroundResource(R.drawable.ic_play_circle_outline_black_72dp);
                }else{
                    pauseButton.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_72dp);
                }
                isPlaying = !isPlaying;
            }
        });

        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // decrease i by 10
                wordCount -= 10;
            }
        });

        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // increase i by 10
                wordCount += 10;
            }
        });

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open the pick to select and import the file
                openFile();
            }
        });

        newsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open up a news feed and be able to select an article to play
                Intent startNewsIntent = new Intent(MainActivity.this, NewsActivity.class);
                startActivity(startNewsIntent);
            }
        });

    }


    public void runLibraryWords(final String bookPath, final long speed) {
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

    public void runNewsWords(final long speed){
        //play each word in String mContent 1 by 1.
        final int newsLen = mContent.length();
        bookProgress.setMax(newsLen);
        final String[] wc = mContent.split(" ");
        final android.os.Handler handler = new android.os.Handler();
        handler.post(new Runnable() {
            int percentage;

            @Override
            public void run() {
                if (wordCount > newsLen)
                    wordCount = newsLen - 1;

                wordTextView.setText(wc[wordCount]);
                wordCount++;
                bookProgress.setProgress(percentage);
                if (wordCount == wc.length) {
                    handler.removeCallbacks(this);
                    bookProgress.setProgress(newsLen);
                } else {
                    if (isPlaying == true) {
                        bookProgress.setProgress(wordCount);
                        handler.postDelayed(this, speed);
                    }
                }
            }
        });
    }


    public void downloadFileAndShowWords(Context context, String fileName, String fileExtension, String destinationDirectory, String url, final long speed) {

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, destinationDirectory, fileName + fileExtension);


        // Local temp file has been created
        //load file into input stream and split each word to display in textview
        String line;
        final String[] downloadPath = {"/storage/emulated/0/Android/data/com.example.mnallamalli97.speedread/files/Download/" + bookPath + ".txt"};

        File tmpDir = new File(downloadPath[0]);
        boolean exists = tmpDir.exists();
        if (!exists) {
            downloadManager.enqueue(request);
        }

        /*
            1000 ms in a second
            60 seconds a minute.

            speed should be: show a word every 250 ms.
        */


        // i want 400 words per min.
        // currently every 400 ms, the word switches.
        try (
                InputStream is = new FileInputStream(downloadPath[0]);
                InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);

        ) {
            //readLine here will be an entire chapter
            line = br.readLine();
            final int len = line.split(" ").length;
            bookProgress.setMax(len);
//            while ((line = br.readLine()) != null) {
            final String[] wc = line.split(" ");
            final android.os.Handler handler = new android.os.Handler();
            handler.post(new Runnable() {
                int percentage;

                @Override
                public void run() {
                    if (wordCount > len)
                        wordCount = len - 1;

                    wordTextView.setText(wc[wordCount]);
                    wordCount++;
                    bookProgress.setProgress(percentage);
                    if (wordCount == wc.length) {
                        handler.removeCallbacks(this);
                        bookProgress.setProgress(len);
                    } else {
                        if (isPlaying == true) {
                            bookProgress.setProgress(wordCount);
                            handler.postDelayed(this, speed);
                        }
                    }
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, PICK_PDF_FILE);
    }


//    void countdown(final int time) {
//        final ArrayList<Integer> count = new ArrayList<>();
//        int j = 0;
//        for(int i = time; i >= 0; i--) {
//            count.add(j, i);
//            j++;
//        }
//
//        final android.os.Handler handler = new android.os.Handler();
//        handler.post(new Runnable() {
//            int i = 0;
//            @Override
//            public void run() {
//                wordTextView.setText((count.get(i)).toString());
//                i++;
//                if (i == time) {
//                    handler.removeCallbacks(this);
//                } else {
//
//                    handler.postDelayed(this, 1000);
//                }
//            }
//        });
//
//    }
}