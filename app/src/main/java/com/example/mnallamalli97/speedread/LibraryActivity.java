package com.example.mnallamalli97.speedread;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.content.ContentValues.TAG;

public class LibraryActivity extends AppCompatActivity {


    private ArrayList<Book> libraryList = new ArrayList<>();
    private ListView listView;
    //SETUP FIREBASE
    DatabaseReference databaseReference;
    ArrayAdapter<Book> adapter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_activity);

        retrieve();
        //ADAPTER
        adapter = new propertyArrayAdapter(this, 0, libraryList);
        //Find list view and bind it with the custom adapter
        listView = findViewById(R.id.customListView);
        listView.setAdapter(adapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(LibraryActivity.this, libraryList.get(position).getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                retrieve(); // your code
                pullToRefresh.setRefreshing(false);
            }
        });

        //add event listener so we can handle clicks
        AdapterView.OnItemClickListener adapterViewListener = new AdapterView.OnItemClickListener() {

            //on click
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Book book = libraryList.get(position);

                Intent intent = new Intent(LibraryActivity.this, SettingsActivity.class);
                intent.putExtra("title", book.getTitle());
                intent.putExtra("author", book.getAuthor());
                intent.putExtra("cover", book.getBookCover());

                startActivity(intent);
            }
        };
        //set the listener to the list view
        listView.setOnItemClickListener(adapterViewListener);
    }



    public void retrieve()
    {
        databaseReference = FirebaseDatabase.getInstance().getReference("speedread")
                .child("books");

        libraryList.clear();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    Book book = ds.getValue(Book.class);
                    System.out.println(book.getTitle());
                    String title = book.getTitle();
                    String author = book.getAuthor();
                    String cover = book.getBookCover();
                    libraryList.add(new Book(title, author, cover));

                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage()); //Don't ignore errors!
            }
        });


    }


    //custom ArrayAdapter
    class propertyArrayAdapter extends ArrayAdapter<Book> {

        private Context context;
        private List<Book> bookList;

        //constructor, call on creation
        public propertyArrayAdapter(Context context, int resource, ArrayList<Book> objects) {
            super(context, resource, objects);

            this.context = context;
            this.bookList = objects;
        }

        //called when rendering the list
        public View getView(int position, View convertView, ViewGroup parent) {

            //get the property we are displaying
            Book book = bookList.get(position);

            //get the inflater and inflate the XML layout for each item
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.book_layout, null);

            TextView title = (TextView) view.findViewById(R.id.bookTitle);
            TextView author = (TextView) view.findViewById(R.id.author);
            ImageView cover = (ImageView) view.findViewById(R.id.cover);

            //set title and author attributes
            title.setText("Title: " + String.valueOf(book.getTitle()));
            author.setText("Author: " + String.valueOf(book.getAuthor()));


            cover.setImageBitmap(BitmapFactory.decodeFile(book.getBookCover()));

            return view;
        }
    }

}
