package com.teammike.iptk.foursquare.utils;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Interface for REST endpoints
 * @author Yadullah Duman
 */
public interface RestClient {

    // -------------------- USER ROUTES -------------------- //
    @GET("user/{username}")
    Call<ResponseBody> getUserByUsername(@Path("username") String username);

    @GET("user/me")
    Call<ResponseBody> getUser();

    @GET("user/{id}/checkIns/king")
    Call<ResponseBody> getVenueKing(@Path("id") String venueId);

    @GET("user/friends/location")
    Call<ResponseBody> getFriendsLocation();

    @POST("user/location")
    Call<ResponseBody> postLocation(@Body RequestBody location);

    @POST("user")
    Call<ResponseBody> createUser(@Body RequestBody user);

    @POST("user/login")
    Call<ResponseBody> login(@Body RequestBody user);

    @POST("user/friends")
    Call<ResponseBody> postFriendRequest(@Body RequestBody username);

    @POST("user/friends/images")
    Call<ResponseBody> postFriendImages(@Body RequestBody friends);

    @Multipart
    @POST("user/me/upload")
    Call<ResponseBody> uploadProfileImage(@Part MultipartBody.Part image);

    @PATCH("user/me")
    Call<ResponseBody> updateUser(@Body RequestBody user);

    @PATCH("user/me/{id}")
    Call<ResponseBody> updateUserById(@Path("id") String userId, @Body RequestBody user);

    @PATCH("user/friends")
    Call<ResponseBody> updateFriendship(@Body RequestBody body);

    @PATCH("user/{id}/checkIns")
    Call<ResponseBody> updateUserCheckIns(@Path("id") String venueId);

    @DELETE("user/me/token")
    Call<ResponseBody> logout();

    @DELETE("user/me/{id}")
    Call<ResponseBody> deleteUserById(@Path("id") String userId);

    @DELETE("user/friends/{username}")
    Call<ResponseBody> deleteFriendshipByUsername(@Path("username") String usernameToUnfriend);


    // -------------------- VENUE ROUTES -------------------- //
    @GET("venues/explore/near/{near}/query/{query}")
    Call<ResponseBody> getVenuesByQuery(@Path("near") String near, @Path("query") String query);

    @GET("venues/explore/near/{near}/category/{category}")
    Call<ResponseBody> getVenuesByCategory(@Path("near") String near, @Path("category") String category);

    @GET("venues/{id}")
    Call<ResponseBody> getVenueById(@Path("id") String venueId);

    @GET("venues/{id}/comment/{pos}")
    Call<ResponseBody> getUserForRatedComment(@Path("id") String venueId, @Path("pos") int pos);

    @GET("venues/{id}/rating")
    Call<ResponseBody> getUserForRating(@Path("id") String venueId);

    @POST("venues/checkins")
    Call<ResponseBody> getCheckedInVenues(@Body RequestBody checkIns);

    @POST("venues/{id}/comment")
    Call<ResponseBody> postComment(@Path("id") String venueId, @Body RequestBody comment);

    @POST("venues/{id}/rating")
    Call<ResponseBody> postRating(@Path("id") String venueId, @Body RequestBody rating);

    @Multipart
    @POST("venues/{id}/comment/upload")
    Call<ResponseBody> uploadCommentImage(@Path("id") String venueId, @Part MultipartBody.Part image);

    @PATCH("venues/{id}/comment")
    Call<ResponseBody> updateComment(@Path("id") String venueId, @Body RequestBody comment);

    @PATCH("venues/{id}/checkins")
    Call<ResponseBody> updateCheckIns(@Path("id") String venueId, @Body RequestBody comment);

}
