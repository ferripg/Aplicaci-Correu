package com.example.aplicaciocorreu;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/register")
    Call<ResponseBody> register(@Body User user);

    @FormUrlEncoded
    @POST("/login")
    Call<TokenResponse> login(
            @Field("username") String user,
            @Field("password") String pass
    );

    @POST("/send-email")
    Call<Void> sendEmail(@Body EmailRequest req);

    @POST("/refresh-token")
    Call<TokenResponse> refreshToken(@Body RefreshTokenRequest request);

    @GET("emails")
    Call<EmailResponse> getEmails();

    @DELETE("/emails/delete")
    Call<ResponseBody> deleteEmail(@Query("uid") String uid);
}