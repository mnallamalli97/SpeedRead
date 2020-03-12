package com.example.mnallamalli97.speedread;

public class Book {
    private String title;
    private String author;
    private String cover;
    private String bookPath;

    public Book() {}

    public Book(String title, String author, String cover, String bookPath) {
        this.title = title;
        this.author = author;
        this.cover = cover;
        this.bookPath = bookPath;
    }

    public String getTitle() { return title; }

    public String getAuthor() {
        return author;
    }

    public String getBookCover() { return cover; }

    public String getBookPath() {
        return bookPath;
    }
}
