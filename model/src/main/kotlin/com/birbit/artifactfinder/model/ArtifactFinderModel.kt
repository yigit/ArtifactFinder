package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.model.db.ArtifactFinderDb
import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo
import java.io.File
import java.util.*


class ArtifactFinderModel internal constructor(private val db: ArtifactFinderDb) {
    constructor(file: File? = null) : this(
        ArtifactFinderDb(
            name = file?.canonicalFile?.absolutePath
        )
    )

    private val artifactDao = db.artifactDao

    @Deprecated("use search with SearchParams parameter")
    @Suppress("unused")
    suspend fun search(query: String): List<SearchRecord> {
        return search(
            SearchParams(
                query = query,
                includeGlobalMethods = false,
                includeClasses = true,
                includeExtensionMethods = false
            )
        )
    }

    @Suppress("unused")
    suspend fun search(params: SearchParams): List<SearchRecord> {
        val senitizedQuery = params.query.trim().replace('.', '$').toLowerCase()
        val classSearch = if (params.includeClasses) {
            artifactDao.searchClasses(senitizedQuery)
        } else {
            emptyList()
        }
        val methodSearchType = ArtifactDao.MethodSearchType.get(
            includeGlobal = params.includeGlobalMethods,
            includeExtension = params.includeExtensionMethods
        )
        val methodSearch = methodSearchType?.let {
            artifactDao.searchMethods(senitizedQuery, it)
        } ?: emptyList()
        return ResultSorter.sort(
            query = params.query,
            results = classSearch + methodSearch
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
                val pieces = classInfo.name.split('$').map { it.toLowerCase(Locale.US) }
                repeat(pieces.size) { limit ->
                    val identifier = pieces.takeLast(limit + 1).joinToString("$")
                    artifactDao.insertClassLookup(
                        ClassLookup(
                            identifier = identifier,
                            classId = localClassId
                        )
                    )
                }
            }
            info.methods.forEach { methodInfo ->
                val methodRecordId = artifactDao.insertMethodRecord(
                    MethodRecord(
                        id = 0,
                        name = methodInfo.name,
                        pkg = methodInfo.pkg,
                        receivePkg = methodInfo.receiver?.pkg,
                        receiveName = methodInfo.receiver?.name,
                        artifactId = pendingArtifact.id
                    )
                )
                artifactDao.insertMethodLookup(
                    MethodLookup(
                        identifier = methodInfo.name.toLowerCase(Locale.US),
                        methodId = methodRecordId
                    )
                )
            }
            artifactDao.markPendingArtifactFetched(pendingArtifact.id)
        }
    }

    class SearchParams(
        val query: String,
        val includeClasses: Boolean,
        val includeExtensionMethods: Boolean,
        val includeGlobalMethods: Boolean
    )
}