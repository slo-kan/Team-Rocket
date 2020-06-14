package com.teamrocket.app.data.network;

import com.teamrocket.app.model.WikiResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface IWikiApi {

    String BASE_URL = "https://en.wikipedia.org/api/rest_v1/";

    @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4164.2 Safari/537.36")
    @GET("page/summary/{name}")
    Call<WikiResponse> getBirdInformation(@Path("name") String name);
}
