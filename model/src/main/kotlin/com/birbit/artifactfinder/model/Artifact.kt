package com.birbit.artifactfinder.model

data class Artifact(
    val id: Long,
    val groupId: String,
    val artifactId: String,
    val version: Version
)
