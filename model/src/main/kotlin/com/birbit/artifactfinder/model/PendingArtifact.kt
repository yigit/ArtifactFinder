package com.birbit.artifactfinder.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index("groupId", "artifactId", "version", unique = true),
        Index("retries")
    ]
)
data class PendingArtifact(
    @PrimaryKey(autoGenerate = true)
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