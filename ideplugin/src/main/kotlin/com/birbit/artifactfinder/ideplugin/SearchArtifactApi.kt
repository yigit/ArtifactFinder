package com.birbit.artifactfinder.ideplugin

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

class SearchArtifactModel {
    private val api = SearchArtifactApi.build()

    suspend fun query(query: String) = withContext(Dispatchers.IO) {
        api.queryResults(query)
    }
}

private interface SearchArtifactApi {
    @GET("searchArtifact")
    suspend fun queryResults(@Query("query") query: String): List<SearchResult>

    companion object {
        fun build(): SearchArtifactApi {
            val contentType = MediaType.get("application/json")

            return Retrofit.Builder()
                .baseUrl("https://birbit.com/")
                .addConverterFactory(Json.asConverterFactory(contentType))
                .build().create(SearchArtifactApi::class.java)
        }
    }
}