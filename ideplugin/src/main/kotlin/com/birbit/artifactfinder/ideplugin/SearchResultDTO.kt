package com.birbit.artifactfinder.ideplugin

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultDTO(
    val score: Int,
    val pkg: String,
    val name: String,
    val groupId: String,
    val receiverName: String?,
    val artifactId: String,
    val version: String
) {
    val artifactDesc by lazy(LazyThreadSafetyMode.NONE) {
        "$groupId:$artifactId"
    }
    val nameDesc by lazy(LazyThreadSafetyMode.NONE) {
        if (receiverName == null) {
            name
        } else {
            "$receiverName.$name"
        }
    }

    fun qualifiedArtifact() = "$groupId:$artifactId:$version"
}