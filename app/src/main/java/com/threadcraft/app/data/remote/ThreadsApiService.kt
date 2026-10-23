package com.threadcraft.app.data.remote

import com.threadcraft.app.data.model.ThreadsContainerStatusResponse
import com.threadcraft.app.data.model.ThreadsCreateResponse
import com.threadcraft.app.data.model.ThreadsLongLivedTokenResponse
import com.threadcraft.app.data.model.ThreadsPublishResponse
import com.threadcraft.app.data.model.ThreadsTokenResponse
import com.threadcraft.app.data.model.ThreadsUserResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ThreadsApiService {

    @FormUrlEncoded
    @POST("oauth/access_token")
    suspend fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("redirect_uri") redirectUri: String,
        @Field("code") code: String
    ): ThreadsTokenResponse

    @GET("access_token")
    suspend fun exchangeLongLivedToken(
        @Query("grant_type") grantType: String = "th_exchange_token",
        @Query("client_secret") clientSecret: String,
        @Query("access_token") accessToken: String
    ): ThreadsLongLivedTokenResponse

    @GET("v1.0/me")
    suspend fun getProfile(
        @Query("fields") fields: String = "id,username,name",
        @Query("access_token") accessToken: String
    ): ThreadsUserResponse

    @POST("v1.0/{user_id}/threads")
    @FormUrlEncoded
    suspend fun createThread(
        @Path("user_id") userId: String,
        @Field("media_type") mediaType: String,
        @Field("access_token") accessToken: String,
        @Field("text") text: String? = null,
        @Field("image_url") imageUrl: String? = null,
        @Field("video_url") videoUrl: String? = null,
        @Field("is_carousel_item") isCarouselItem: Boolean? = null,
        @Field("children") children: String? = null,
        @Field("reply_to_id") replyToId: String? = null
    ): ThreadsCreateResponse

    @GET("v1.0/{container_id}")
    suspend fun getContainerStatus(
        @Path("container_id") containerId: String,
        @Query("fields") fields: String = "id,status,error_message",
        @Query("access_token") accessToken: String
    ): ThreadsContainerStatusResponse

    @POST("v1.0/{user_id}/threads_publish")
    @FormUrlEncoded
    suspend fun publishThread(
        @Path("user_id") userId: String,
        @Field("creation_id") creationId: String,
        @Field("access_token") accessToken: String
    ): ThreadsPublishResponse
}
