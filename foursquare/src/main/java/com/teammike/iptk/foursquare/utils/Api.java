package com.teammike.iptk.foursquare.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

/**
 * Singleton class to build Retrofit object
 * @author Yadullah Duman
 */
public class Api {

    private static Api instance;
    private RestClient restClient;

    public static synchronized Api getInstance() {
        if (instance == null || AppHandler.getToken().equals("")) {
            instance = new Api();
        }
        return instance;
    }

    public RestClient getClient() {
        return restClient;
    }

    private Api() {
        buildRetrofit();
    }

    private void buildRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    Request.Builder newRequest = request.newBuilder()
                            .header("x-auth", AppHandler.getToken());
                    return chain.proceed(newRequest.build());
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_DEV)
                .client(okHttpClient)
                .build();
        restClient = retrofit.create(RestClient.class);
    }

}
