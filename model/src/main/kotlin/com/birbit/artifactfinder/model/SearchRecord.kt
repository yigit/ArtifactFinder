package com.birbit.artifactfinder.model

data class SearchRecord(
    val pkg: String,
    val name: String,
    val type: Type,
    val groupId: String,
    val artifactId: String,
    val version: Version
) : Comparable<SearchRecord> {
    var score: Int = 0
    override fun compareTo(other: SearchRecord): Int {
        val scopeCmp = score.compareTo(other.score)
        if (scopeCmp != 0) {
            return -scopeCmp
        }
        return -(version.compareTo(other.version))
    }

    enum class Type {
        CLASS,
        GLOBAL_METHOD,
        EXTENSION_METHOD
    }
}