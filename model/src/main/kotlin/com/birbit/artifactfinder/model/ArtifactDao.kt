package com.birbit.artifactfinder.model

internal interface ArtifactDao {
    suspend fun insertArtifact(artifact: Artifact): Long

    suspend fun findArtifact(
        groupId: String,
        artifactId: String,
        version: Version
    ): Artifact?

    suspend fun insertClassRecord(classRecord: ClassRecord): Long

    suspend fun insertClassLookup(classLookup: ClassLookup)

    suspend fun search(query: String): List<SearchRecord>

    suspend fun allLookups(): List<ClassLookup>

    suspend fun insertPendingArtifact(pendingArtifact: PendingArtifact)

    suspend fun findPendingArtifact(
        groupId: String,
        artifactId: String,
        version: Version
    ): Artifact?

    suspend fun incrementPendingArtifactRetry(id: Long)

    suspend fun markPendingArtifactFetched(id: Long)

    suspend fun findNextPendingArtifact(excludeIds: List<Long>): PendingArtifact?

    suspend fun deleteArtifact(artifact: Artifact)

    suspend fun deleteClassRecord(classRecord: ClassRecord)
}