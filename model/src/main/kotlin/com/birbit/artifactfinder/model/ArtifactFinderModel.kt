package com.birbit.artifactfinder.model

import androidx.room.withTransaction
import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo
import java.io.File
import java.util.concurrent.Executors

class ArtifactFinderModel internal constructor(db: ArtifactFinderDatabase) {

    private val dbProvider = DatabaseProvider(db)
    constructor(db:File? = null) : this(
        ArtifactFinderDatabase.create(
            name = db?.canonicalFile?.absolutePath,
            readExecutor = Executors.newSingleThreadExecutor(),
            writeExecutor = Executors.newSingleThreadExecutor()
        )
    )

    suspend fun search(query: String) = dbProvider.use {
        val results = artifactDao().search(query.trim().replace('.', '$').toLowerCase())
        return@use ResultSorter.sort(
            query = query,
            results = results
        )
    }


    suspend fun addPendingArtifact(
        groupId: String,
        artifactId: String,
        version: Version
    ): Boolean {
        return dbProvider.use {
            withTransaction {
                val existing = artifactDao().findPendingArtifact(
                    groupId = groupId,
                    artifactId = artifactId,
                    version = version
                )
                if (existing != null) {
                    return@withTransaction false
                }
                artifactDao().insertPendingArtifact(
                    PendingArtifact(
                        id = 0,
                        groupId = groupId,
                        artifactId = artifactId,
                        version = version,
                        retries = 0,
                        fetched = false
                    )
                )
                return@withTransaction true
            }
        }
    }

    suspend fun findNextPendingArtifact(excludeIds : List<Long>) = dbProvider.use {
        artifactDao().findNextPendingArtifact(excludeIds)
    }

    suspend fun incrementPendingArtifactRetry(
        pendingArtifact: PendingArtifact
    ) = dbProvider.use {
        artifactDao().incrementPendingArtifactRetry(pendingArtifact.id)
    }

    suspend fun sync() {
        dbProvider.use {
            this.compileStatement("PRAGMA wal_checkpoint(TRUNCATE)").execute()
        }
    }

    suspend fun saveParsedArtifact(
        pendingArtifact: PendingArtifact,
        info: ParsedArtifactInfo
    ) {
        dbProvider.use {
            withTransaction {
                val existing = artifactDao().findArtifact(
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
                artifactDao().insertArtifact(
                    pendingArtifact.toArtifact()
                )
                info.classes.forEach { classInfo ->
                    val localClassId = artifactDao().insertClassRecord(
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
                        artifactDao().insertClassLookup(
                            ClassLookup(
                                rowId = 0,
                                identifier = identifier,
                                classId = localClassId
                            )
                        )
                    }
                }
                artifactDao().markPendingArtifactFetched(pendingArtifact.id)
            }
        }
    }
}