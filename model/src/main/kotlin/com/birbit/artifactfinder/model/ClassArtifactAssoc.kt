package com.birbit.artifactfinder.model

import androidx.room.Entity

/**
 * Holds the association between a ClassRecord and an Artifact
 */
@Entity(
    primaryKeys = ["classId", "artifactId"]
)
data class ClassArtifactAssoc(
    val classId: Long,
    val artifactId: Long
)