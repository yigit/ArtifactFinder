package com.birbit.artifactfinder.model

data class MethodRecord(
    val id: Long,
    val pkg: String,
    val name: String,
    val receivePkg: String?,
    val receiveName: String?,
    val artifactId: Long
)