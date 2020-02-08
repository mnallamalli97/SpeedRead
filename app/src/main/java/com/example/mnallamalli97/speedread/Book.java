package com.example.mnallamalli97.speedread;

import android.media.Image;

public class Book {
    private String title;
    private String author;
    private String bookCover;

    public Book(String title, String author, String bookCover) {
        this.title = title;
        this.author = author;
        this.bookCover = bookCover;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getBookCover() {
        return bookCover;
    }
}
