package com.birbit.artifactfinder.ideplugin

/**
 * processes search results for UI representation
 */
class SearchResultModel private constructor(
    val items: List<SearchResult>
) {

    data class SearchResult(
        val desc: String,
        val artifactDesc: String,
        val groupId: String,
        val artifactId: String,
        val versions : MutableList<String> = mutableListOf()
    ) {
        fun qualifiedArtifact(version:String) = "$groupId:$artifactId:$version"
    }


    companion object {
        val EMPTY = SearchResultModel(emptyList())
        fun fromSearchResultDTO(rawResults: List<SearchResultDTO>): SearchResultModel {
            val items = LinkedHashMap<String, SearchResult>()
            rawResults.forEach { rawResult ->
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
                items.values.toList()
            )
        }
    }
}