package com.birbit.artifactfinder.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index("groupId", "artifactId", "version", unique = true)
    ]
)
data class Artifact(
    @PrimaryKey(autoGenerate = true)
    val id:Long,
    val groupId: String,
    val artifactId: String,
    val version: Version
)
