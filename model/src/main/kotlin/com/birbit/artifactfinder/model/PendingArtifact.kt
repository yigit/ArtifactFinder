package com.birbit.artifactfinder.model

data class PendingArtifact(
    val id: Long,
    val groupId: String,
    val artifactId: String,
    val version: Version,
    val retries: Int = 0,
    val fetched: Boolean = false
) {
    fun toArtifact() = Artifact(
        id = id,
        groupId = groupId,
        artifactId = artifactId,
        version = version
    )
}