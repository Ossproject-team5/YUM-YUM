package com.example.ossw5team;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RemoteService {
    public static final String BASE_URL="http://192.168.0.13:8088/smTown/";

    @GET("restaurantlist.jsp")
    Call<List<RestaurantVO>> listRestaurant();

    @GET("restaurantlistsch.jsp")
    Call<List<RestaurantVO>> schRestaurant(
            @Query("query") String query);
}
