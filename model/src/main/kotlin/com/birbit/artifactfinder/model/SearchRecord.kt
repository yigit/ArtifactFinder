package com.birbit.artifactfinder.model

import androidx.room.Ignore

data class SearchRecord(
    val pkg: String,
    val name: String,
    val groupId: String,
    val artifactId: String,
    val version: Version
) : Comparable<SearchRecord> {
    @Ignore
    var score: Int = 0
    override fun compareTo(other: SearchRecord): Int {
        val scopeCmp = score.compareTo(other.score)
        if (scopeCmp != 0) {
            return - scopeCmp
        }
        return - (version.compareTo(other.version))
    }

}