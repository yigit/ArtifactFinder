package com.birbit.artifactfinder.model

import androidx.room.Embedded

data class SearchRecord(
    val pkg:String,
    val name:String,
    val groupId:String,
    val artifactId:String,
    val version:Version
)