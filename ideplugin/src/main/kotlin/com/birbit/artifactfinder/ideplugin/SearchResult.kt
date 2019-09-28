package com.birbit.artifactfinder.ideplugin

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val score: Int,
    val pkg: String,
    val name: String,
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    val artifactDesc by lazy {
        "$groupId:$artifactId"
    }

    fun qualifiedArtifact() = "$groupId:$artifactId:$version"
}