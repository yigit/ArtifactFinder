package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.model.db.ArtifactFinderDb
import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo
import java.io.File


class ArtifactFinderModel internal constructor(private val db: ArtifactFinderDb) {
    constructor(file: File? = null) : this(
        ArtifactFinderDb(
            name = file?.canonicalFile?.absolutePath
        )
    )

    private val artifactDao = db.artifactDao

    @Suppress("unused")
    suspend fun search(query: String): List<SearchRecord> {
        val results = artifactDao.search(query.trim().replace('.', '$').toLowerCase())
        return ResultSorter.sort(
            query = query,
            results = results
        )
    }


    suspend fun addPendingArtifact(
        groupId: String,
        artifactId: String,
        version: Version
    ): Boolean {
        return db.withTransaction {
            val existing = db.artifactDao.findPendingArtifact(
                groupId = groupId,
                artifactId = artifactId,
                version = version
            )
            if (existing != null) {
                false
            } else {
                artifactDao.insertPendingArtifact(
                    PendingArtifact(
                        id = 0,
                        groupId = groupId,
                        artifactId = artifactId,
                        version = version,
                        retries = 0,
                        fetched = false
                    )
                )
                true
            }
        }
    }

    suspend fun findNextPendingArtifact(excludeIds: List<Long>) = artifactDao.findNextPendingArtifact(excludeIds)

    suspend fun incrementPendingArtifactRetry(
        pendingArtifact: PendingArtifact
    ) = artifactDao.incrementPendingArtifactRetry(pendingArtifact.id)

    suspend fun sync() {
        db.exec("PRAGMA wal_checkpoint(TRUNCATE)")
    }

    suspend fun saveParsedArtifact(
        pendingArtifact: PendingArtifact,
        info: ParsedArtifactInfo
    ) {
        db.withTransaction {
            val existing = artifactDao.findArtifact(
                groupId = pendingArtifact.groupId,
                artifactId = pendingArtifact.artifactId,
                version = pendingArtifact.version
            )
            if (existing != null) {
                return@withTransaction
            }
            check(pendingArtifact.id > 0) {
                "pending artifact must be from db"
            }
            artifactDao.insertArtifact(
                pendingArtifact.toArtifact()
            )
            info.classes.forEach { classInfo ->
                val localClassId = artifactDao.insertClassRecord(
                    ClassRecord(
                        id = 0,
                        pkg = classInfo.pkg,
                        name = classInfo.name,
                        artifactId = pendingArtifact.id
                    )
                )
                val pieces = classInfo.name.split('$').map { it.toLowerCase() }
                repeat(pieces.size) { limit ->
                    val identifier = pieces.takeLast(limit + 1).joinToString("$")
                    artifactDao.insertClassLookup(
                        ClassLookup(
                            rowId = 0,
                            identifier = identifier,
                            classId = localClassId
                        )
                    )
                }
            }
            artifactDao.markPendingArtifactFetched(pendingArtifact.id)
        }
    }
}