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

/**
 * processes search results for UI representation
 */
class SearchResultModel private constructor(
    val latestVersion: Int,
    val items: List<SearchResult>
) {

    data class SearchResult(
        val desc: String,
        val artifactDesc: String,
        val groupId: String,
        val artifactId: String,
        val versions: MutableList<String> = mutableListOf()
    ) {
        fun qualifiedArtifact(version: String) = "$groupId:$artifactId:$version"
    }

    companion object {
        val EMPTY = SearchResultModel(0, emptyList())
        fun fromSearchResponse(response: SearchResponseDTO): SearchResultModel {
            val items = LinkedHashMap<String, SearchResult>()
            response.results.forEach { rawResult ->
                val result = items.getOrPut(rawResult.nameDesc + " " + rawResult.artifactDesc) {
                    SearchResult(
                        desc = rawResult.nameDesc,
                        artifactDesc = rawResult.artifactDesc,
                        groupId = rawResult.groupId,
                        artifactId = rawResult.artifactId
                    )
                }
                result.versions.add(rawResult.version)
            }
            return SearchResultModel(
                latestVersion = response.latestVersion,
                items = items.values.toList()
            )
        }
    }
}
