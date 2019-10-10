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

package com.birbit.artifactfinder.webservice

import com.birbit.artifactfinder.dto.SearchResponseDTO
import com.birbit.artifactfinder.dto.SearchResultDTO
import com.birbit.artifactfinder.model.ArtifactFinderModel
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import java.io.File
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@Suppress("unused")
@UnstableDefault
class ArtifactFinderRequestHandler(
    dbFile: File
) {
    private val model by lazy {
        ArtifactFinderModel(dbFile)
    }

    suspend fun handleArtifactFinderRequest(call: ApplicationCall) {
        call.handleRequest()
    }

    private suspend fun ApplicationCall.handleRequest() {
        val query = parameters["query"]
        val limit = (parameters["limit"]?.toIntOrNull() ?: 20).coerceAtMost(50)
        @Suppress("UNUSED_VARIABLE") // one day
        val version = parameters["version"]?.toIntOrNull() ?: 1
        val includeClasses = parameters["includeClasses"].parseBoolean(true)
        val includeExtensionMethods = parameters["includeExtensionMethods"].parseBoolean(false)
        val includeGlobalMethods = parameters["includeGlobalMethods"].parseBoolean(false)
        when {
            query == null -> respond(
                status = HttpStatusCode.BadRequest,
                message = "need a 'query' parameter"
            )
            query.isEmpty() -> respond(
                status = HttpStatusCode.OK,
                message = SearchResponseDTO(
                    latestVersion = 1,
                    results = emptyList()
                )
            )
            else -> {
                val items = model.search(
                    ArtifactFinderModel.SearchParams(
                        query = query,
                        includeClasses = includeClasses,
                        includeExtensionMethods = includeExtensionMethods,
                        includeGlobalMethods = includeGlobalMethods
                    )
                ).take(limit).map {
                    SearchResultDTO(
                        pkg = it.pkg,
                        name = it.name,
                        receiverName = it.receiverName,
                        groupId = it.groupId,
                        artifactId = it.artifactId,
                        version = it.version.toString(),
                        score = it.score
                    )
                }
                val json = Json.stringify(
                    serializer = SearchResponseDTO.serializer(),
                    obj = SearchResponseDTO(
                        latestVersion = 1,
                        results = items
                    )
                )
                respondText(
                    contentType = JSON_CONTENT_TYPE,
                    status = HttpStatusCode.OK,
                    text = json
                )
            }
        }
    }

    private fun String?.parseBoolean(default: Boolean): Boolean {
        if (this == null) {
            return default
        }
        val trimmed = trim()
        if (trimmed.toLowerCase() == "true") {
            return true
        }
        return trimmed.toIntOrNull()?.let {
            it > 0
        } ?: default
    }

    companion object {
        private val JSON_CONTENT_TYPE = ContentType.parse("application/json")
    }
}
