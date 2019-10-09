package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.vo.Artifactory

data class Artifact(
    val id: Long,
    val groupId: String,
    val artifactId: String,
    val version: Version,
    val artifactory: Artifactory
)
