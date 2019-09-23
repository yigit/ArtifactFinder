package com.birbit.artifactfinder.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ClassRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    // . separated
    val pkg: String,
    // $ separated
    val name: String,
    val artifactId: Long
) {
}