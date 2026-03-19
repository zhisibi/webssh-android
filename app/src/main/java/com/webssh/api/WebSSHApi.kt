package com.webssh.api

import retrofit2.Response
import retrofit2.http.*

interface WebSSHApi {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<LogoutResponse>

    @GET("api/servers")
    suspend fun getServers(@Header("Authorization") token: String): Response<List<Server>>

    @POST("api/servers")
    suspend fun addServer(
        @Header("Authorization") token: String,
        @Body server: ServerRequest
    ): Response<ServerResponse>

    @PUT("api/servers/{id}")
    suspend fun updateServer(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body server: ServerRequest
    ): Response<ServerResponse>

    @DELETE("api/servers/{id}")
    suspend fun deleteServer(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<ServerResponse>

    @GET("api/sftp/list")
    suspend fun listFiles(
        @Header("Authorization") token: String,
        @Query("serverId") serverId: Long,
        @Query("path") path: String
    ): Response<FileListResponse>

    @POST("api/sftp/mkdir")
    suspend fun createFolder(
        @Header("Authorization") token: String,
        @Body request: MkdirRequest
    ): Response<BaseResponse>

    @POST("api/sftp/delete")
    suspend fun deleteFile(
        @Header("Authorization") token: String,
        @Body request: DeleteRequest
    ): Response<BaseResponse>

    @POST("api/sftp/rename")
    suspend fun renameFile(
        @Header("Authorization") token: String,
        @Body request: RenameRequest
    ): Response<BaseResponse>

    @POST("api/sftp/download-batch")
    suspend fun downloadBatch(
        @Header("Authorization") token: String,
        @Body request: DownloadBatchRequest
    ): Response<BaseResponse>
}

// Request/Response Models
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String, val token: String?)
data class LogoutResponse(val success: Boolean, val message: String)
data class BaseResponse(val success: Boolean, val message: String?)

data class Server(
    val id: Long,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val authType: String,
    val tags: List<String>,
    val enabled: Boolean
)

data class ServerRequest(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val authType: String,
    val password: String,
    val tags: List<String>,
    val enabled: Boolean
)

data class ServerResponse(val success: Boolean, val message: String, val id: Long?)

data class FileItem(
    val name: String,
    val type: String, // "directory", "file", "link"
    val size: Long,
    val mtime: Long,
    val mode: String
)

data class FileListResponse(val success: Boolean, val files: List<FileItem>?)

data class MkdirRequest(val serverId: Long, val path: String, val dirname: String)
data class DeleteRequest(val serverId: Long, val targetPath: String, val type: String)
data class RenameRequest(val serverId: Long, val oldPath: String, val newPath: String)
data class DownloadBatchRequest(val serverId: Long, val paths: List<String>)
