package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.vo.Artifactory

data class PendingArtifact(
    val id: Long,
    val groupId: String,
    val artifactId: String,
    val version: Version,
    val retries: Int = 0,
    val fetched: Boolean = false,
    val artifactory: Artifactory
) {
    fun toArtifact() = Artifact(
        id = id,
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        artifactory = artifactory
    )
}