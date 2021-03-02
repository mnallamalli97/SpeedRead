package com.example.mnallamalli97.speedread.api;

import com.example.mnallamalli97.speedread.models.News;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiInterface {

    @GET("top-headlines")
    Call<News> getNews(

            @Query("country") String country ,
            @Query("apiKey") String apiKey

    );

    @GET("top-headlines")
    Call<News> getNewsSearch(
            @Query("language") String language,
            @Query("category") String category,
            @Query("sortBy") String sortBy,
            @Query("apiKey") String apiKey

    );

}
