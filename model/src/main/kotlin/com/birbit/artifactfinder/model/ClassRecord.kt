package com.birbit.artifactfinder.model

data class ClassRecord(
    val id: Long,
    // . separated
    val pkg: String,
    // $ separated
    val name: String,
    val artifactId: Long
) {
}