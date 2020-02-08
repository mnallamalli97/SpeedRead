package com.example.mnallamalli97.speedread;

import android.provider.ContactsContract;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class FirebaseHelper {

    DatabaseReference db;
    Boolean saved=null;
    ArrayList<Book> bookList = new ArrayList<>();

    public FirebaseHelper(DatabaseReference db) {
        this.db = db;
    }

    //READ
    public ArrayList<Book> retrieve()
    {
        ChildEventListener childEventListener = db.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                fetchData(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                fetchData(dataSnapshot);

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return bookList;
    }

    private void fetchData(DataSnapshot dataSnapshot)
    {
        bookList.clear();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference booksRef = rootRef.child("speedread").child("books");

        for (DataSnapshot ds : dataSnapshot.getChildren())
        {

           //String author = ds.child("author").getValue(String.class);
            //String title = ds.getValue(Book.class).getTitle().toString();

            //String cover = ds.getValue(Book.class).getBookCover().toString();

            bookList.add(new Book("this", "is", "thor"));

        }
    }
}