package com.birbit.artifactfinder.model

import androidx.room.withTransaction
import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo

class ArtifactFinderModel {
    private val db:ArtifactFinderDatabase

    internal constructor(db : ArtifactFinderDatabase) {
        this.db = db
    }

    suspend fun search(query:String) = db
        .artifactDao()
        .search(query.trim().replace('.', '$').toLowerCase())

    suspend fun saveParsedArtifact(
        groupId: String,
        artifactId: String,
        version: Version,
        info: ParsedArtifactInfo) {
        db.withTransaction {
            val existing = db.artifactDao().findArtifact(
                groupId = groupId,
                artifactId = artifactId,
                version = version
            )
            if (existing != null) {
                return@withTransaction
            }
            val localArtifactId = db.artifactDao().insertArtifact(
                Artifact(
                id = 0,
                groupId = groupId,
                    artifactId = artifactId,
                    version = version
            ))
            info.classes.forEach {classInfo ->
                val localClassId = db.artifactDao().insertClassRecord(
                    ClassRecord(
                        id = 0,
                        pkg = classInfo.pkg,
                        name = classInfo.name,
                        artifactId = localArtifactId
                    )
                )
                val pieces = classInfo.name.split('$').map { it.toLowerCase() }
                repeat(pieces.size) { limit ->
                    val identifier = pieces.takeLast(limit + 1).joinToString("$")
                    db.artifactDao().insertClassLookup(
                        ClassLookup(
                            rowId = 0,
                            identifier = identifier,
                            classId = localClassId
                        )
                    )
                }
            }
        }
    }
}