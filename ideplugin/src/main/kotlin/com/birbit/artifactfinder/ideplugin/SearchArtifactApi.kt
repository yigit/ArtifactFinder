/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.birbit.artifactfinder.ideplugin

import com.birbit.artifactfinder.dto.SearchResponseDTO
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
        SearchResultModel.fromSearchResponse(api.queryResults(query))
    }
}

private interface SearchArtifactApi {
    @GET("searchArtifact")
    suspend fun queryResults(
        @Query("query") query: String,
        @Query("includeClasses") includeClasses: Boolean = true,
        @Query("includeExtensionMethods") includeExtensionMethods: Boolean = true,
        @Query("includeGlobalMethods") includeGlobalMethods: Boolean = true,
        @Query("version") version: Int = 1
    ): SearchResponseDTO

    companion object {
        fun build(): SearchArtifactApi {
            val contentType = MediaType.get("application/json")

            return Retrofit.Builder()
                .baseUrl("https://birbit.com/")
                .addConverterFactory(Json.nonstrict.asConverterFactory(contentType))
                .build().create(SearchArtifactApi::class.java)
        }
    }
}
