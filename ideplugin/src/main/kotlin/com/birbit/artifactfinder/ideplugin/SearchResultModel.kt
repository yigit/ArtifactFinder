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