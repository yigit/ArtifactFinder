package com.birbit.artifactfinder.model.db

import com.birbit.artifactfinder.model.*
import java.sql.DriverManager

internal class ArtifactDaoImpl(
    private val conn: ConnectionProvider
) : ArtifactDao {
    override suspend fun deleteClassRecord(classRecord: ClassRecord) {
        check(classRecord.id >= 0) {
            "Invalid class record id ${classRecord.id}"
        }
        conn.write {
            prepareWrite(
                """
                DELETE FROM $CLASS_RECORD WHERE $ID = ?
            """.trimIndent()
            ).also {
                it.bindLong(1, classRecord.id)
            }.exec()
        }
    }

    override suspend fun deleteArtifact(artifact: Artifact) {
        check(artifact.id >= 0) {
            "Invalid artifact id ${artifact.id}"
        }
        conn.write {
            prepareWrite(
                """
                DELETE FROM $ARTIFACT WHERE $ID = ?
            """.trimIndent()
            ).also {
                it.bindLong(1, artifact.id)
            }.exec()
        }
    }

    override suspend fun insertArtifact(artifact: Artifact): Long {
        return conn.write {
            this.prepareWrite(
                """
                INSERT OR REPLACE INTO `Artifact` (`$ID`,`$GROUP_ID`,`$ARTIFACT_ID`,`$VERSION`) VALUES (nullif(?, 0),?,?,?)
            """.trimIndent()
            ).also {
                it.bindLong(1, artifact.id)
                it.bindString(2, artifact.groupId)
                it.bindString(3, artifact.artifactId)
                it.bindString(4, artifact.version.toString())
            }.execForLastRowId()
        }
    }

    override suspend fun findArtifact(groupId: String, artifactId: String, version: Version): Artifact? {
        return conn.read {
            prepareRead(
                """
                SELECT * FROM Artifact
                WHERE $GROUP_ID = ? AND $ARTIFACT_ID = ? AND $VERSION = ?
                LIMIT 1
            """.trimIndent()
            ).also { query ->
                query.bindString(1, groupId)
                query.bindString(2, artifactId)
                query.bindString(3, version.toString())
            }.query { rs ->
                if (rs.nextRow()) {
                    rs.asArtifact()
                } else {
                    null
                }
            }
        }
    }

    override suspend fun insertClassRecord(classRecord: ClassRecord): Long {
        return conn.write {
            prepareWrite(
                """
                INSERT OR REPLACE INTO `ClassRecord` (`$ID`,`$PKG`,`$NAME`,`$ARTIFACT_ID`) VALUES (nullif(?, 0),?,?,?)
            """.trimIndent()
            ).also { query ->
                query.bindLong(1, classRecord.id)
                query.bindString(2, classRecord.pkg)
                query.bindString(3, classRecord.name)
                query.bindLong(4, classRecord.artifactId)
            }.execForLastRowId()
        }
    }

    override suspend fun insertClassLookup(classLookup: ClassLookup) {
        conn.write {
            prepareWrite(
                """
                    INSERT OR REPLACE INTO `ClassLookup` (`$IDENTIFIER`,`$CLASS_ID`) VALUES (?,?)
                """.trimIndent()
            ).also { query ->
                query.bindString(1, classLookup.identifier)
                query.bindLong(2, classLookup.classId)
            }.exec()
        }
    }

    override suspend fun search(query: String): List<SearchRecord> {
        return conn.read {
            prepareRead(
                """
                SELECT cr.$NAME, cr.$PKG, a.$ARTIFACT_ID, a.$GROUP_ID, a.$VERSION
                    FROM ClassLookup cl, Artifact a, ClassRecord cr
                    WHERE cl.identifier LIKE ? || '%'
                        AND cl.classId = cr.id
                        AND cr.artifactId = a.id
                """.trimIndent()
            ).also {
                it.bindString(1, query)
            }.query { result ->
                result.asSearchRecords()
            }
        }
    }

    override suspend fun allLookups(): List<ClassLookup> {
        return conn.read {
            prepareRead("SELECT * FROM ClassLookup").query {
                it.asClassLookups()
            }
        }
    }

    override suspend fun insertPendingArtifact(pendingArtifact: PendingArtifact) {
        conn.write {
            prepareWrite(
                """
                INSERT OR IGNORE INTO `PendingArtifact` (`$ID`,`$GROUP_ID`,`$ARTIFACT_ID`,`$VERSION`,`$RETRIES`,`$FETCHED`)
                VALUES (nullif(?, 0),?,?,?,?,?)
            """.trimIndent()
            ).also {
                it.bindLong(1, pendingArtifact.id)
                it.bindString(2, pendingArtifact.groupId)
                it.bindString(3, pendingArtifact.artifactId)
                it.bindString(4, pendingArtifact.version.toString())
                it.bindInt(5, pendingArtifact.retries)
                it.bindBoolean(6, pendingArtifact.fetched)
            }.exec()
        }
    }

    override suspend fun findPendingArtifact(groupId: String, artifactId: String, version: Version): Artifact? {
        return conn.read {
            prepareRead(
                """
                SELECT * FROM PendingArtifact
                WHERE $GROUP_ID = ? AND $ARTIFACT_ID = ? AND $VERSION = ?
                LIMIT 1
                """.trimIndent()
            ).also {
                it.bindString(1, groupId)
                it.bindString(2, artifactId)
                it.bindString(3, version.toString())
            }
        }.query {
            if (it.nextRow()) {
                it.asArtifact()
            } else {
                null
            }
        }
    }

    override suspend fun incrementPendingArtifactRetry(id: Long) {
        conn.write {
            prepareWrite(
                """
                UPDATE PendingArtifact
                SET retries = retries + 1
                WHERE id = ?
            """.trimIndent()
            ).also {
                it.bindLong(1, id)
            }.exec()
        }
    }

    override suspend fun markPendingArtifactFetched(id: Long) {
        conn.write {
            prepareWrite(
                """
                UPDATE PendingArtifact
                SET fetched = 1
                WHERE id = ?
            """.trimIndent()
            ).also {
                it.bindLong(1, id)
            }.exec()
        }
    }

    override suspend fun findNextPendingArtifact(excludeIds: List<Long>): PendingArtifact? {
        val idsParam = if (excludeIds.isEmpty()) {
            "-1"
        } else {
            excludeIds.joinToString(",")
        }
        return conn.read {
            prepareRead(
                """
                SELECT * FROM PendingArtifact
                WHERE fetched = 0 AND retries < 20 AND id NOT IN($idsParam)
                ORDER BY retries DESC
                LIMIT 1
            """.trimIndent()
            )
        }.query {
            if (it.nextRow()) {
                it.asPendingArtifact()
            } else {
                null
            }
        }
    }

    private fun QueryResult.asArtifact() = Artifact(
        id = getLong(ID),
        groupId = getString(GROUP_ID),
        artifactId = getString(ARTIFACT_ID),
        version = Version.fromString(getString(VERSION))!!
    )

    private fun QueryResult.asSearchRecords() = asSequence().map {
        SearchRecord(
            pkg = getString(PKG),
            name = getString(NAME),
            groupId = getString(GROUP_ID),
            artifactId = getString(ARTIFACT_ID),
            version = Version.fromString(getString(VERSION))!!
        )
    }.toList()

    private fun QueryResult.asClassLookups() = asSequence().map {
        ClassLookup(
            rowId = 0,//TODO remove after cleaning room
            identifier = getString(IDENTIFIER),
            classId = getLong(CLASS_ID)
        )
    }.toList()

    private fun QueryResult.asPendingArtifact() = PendingArtifact(
        id = getLong(ID),
        groupId = getString(GROUP_ID),
        artifactId = getString(ARTIFACT_ID),
        version = Version.fromString(getString(VERSION))!!,
        retries = getInt(RETRIES),
        fetched = getBoolean(FETCHED)
    )

    companion object {
        // table names
        internal const val ARTIFACT = "Artifact"
        internal const val CLASS_RECORD = "ClassRecord"
        internal const val CLASS_LOOKUP = "ClassLookup"
        internal const val PENDING_ARTIFACT = "PendingArtifact"
        // column names
        internal const val ID = "id"
        internal const val ARTIFACT_ID = "artifactId"
        internal const val GROUP_ID = "groupId"
        internal const val VERSION = "version"
        internal const val PKG = "pkg"
        internal const val NAME = "name"
        internal const val IDENTIFIER = "identifier"
        internal const val CLASS_ID = "classId"
        internal const val RETRIES = "retries"
        internal const val FETCHED = "fetched"
    }
}

class ArtifactFinderDb(
    name: String? = null
) {
    private val connectionProvider = ConnectionProvider(
        limit = if (name == null) 1 else 4,
        createConnection = {
            val id = name ?: ":memory:"
            JdbcWriteableDbDriver(DriverManager.getConnection("jdbc:sqlite:$id?journal_mode=WAL")).also {
                it.conn.prepareStatement("PRAGMA foreign_keys = ON;").execute()
                it.conn.prepareStatement("PRAGMA case_sensitive_like=ON;").execute()
            }
        },
        initFirstDb = {
            createAllTables(it)
        }
    )

    suspend fun <T> withTransaction(block: suspend () -> T) = connectionProvider.write {
        this.withTransaction {
            block()
        }
    }

    suspend fun exec(sql: String) = connectionProvider.write {
        exec(sql)
    }

    suspend fun <T> query(sql: String, block: (QueryResult) -> T) = connectionProvider.read {
        this.prepareRead(sql).query(block)
    }

    internal val artifactDao: ArtifactDao = ArtifactDaoImpl(connectionProvider)


    companion object {
        suspend fun createAllTables(writableDbDriver: WritableDbDriver) {
            writableDbDriver.withTransaction {
                val migrations = listOf(Migration_1())
                val version = writableDbDriver.prepareRead("""
                    PRAGMA user_version
                """.trimIndent()).query {
                    if(it.nextRow()) {
                        it.getInt("user_version")
                    } else {
                        0
                    }
                }
                val selected = migrations.filter {
                    it.endVersion > version
                }
                if (selected.isNotEmpty()) {
                    selected.forEach {
                        it.apply(writableDbDriver)
                    }
                    writableDbDriver.exec("""
                    PRAGMA user_version = ${selected.last().endVersion}
                """.trimIndent())
                }
            }

        }
    }

    internal interface Migration {
        val endVersion: Int
        suspend fun apply(writableDbDriver: WritableDbDriver)
    }


    internal class Migration_1 : Migration {
        override suspend fun apply(writableDbDriver: WritableDbDriver) {
            writableDbDriver.exec(
                """
                        CREATE TABLE IF NOT EXISTS `Artifact` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `groupId` TEXT NOT NULL,
                            `artifactId` TEXT NOT NULL,
                            `version` TEXT NOT NULL)
                    """.trimIndent()
            )
            writableDbDriver.exec(
                """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_Artifact_groupId_artifactId_version`
                    ON `Artifact` (`groupId`, `artifactId`, `version`)
                """.trimIndent()
            )
            writableDbDriver.exec(
                """
                    CREATE TABLE IF NOT EXISTS `ClassLookup` (
                        `identifier` TEXT NOT NULL,
                        `classId` INTEGER NOT NULL,
                        PRIMARY KEY(`identifier`, `classId`),
                        FOREIGN KEY(classId) REFERENCES ClassRecord(id) ON DELETE CASCADE ON UPDATE CASCADE
                    ) WITHOUT ROWID
                """.trimIndent()
            )
            writableDbDriver.exec(
                """
                    CREATE TABLE IF NOT EXISTS `ClassRecord` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `pkg` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `artifactId` INTEGER NOT NULL REFERENCES Artifact(id) ON DELETE CASCADE ON UPDATE CASCADE DEFERRABLE)
                """.trimIndent()
            )
            writableDbDriver.exec(
                """
                    CREATE TABLE IF NOT EXISTS `PendingArtifact` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `groupId` TEXT NOT NULL,
                        `artifactId` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `retries` INTEGER NOT NULL,
                        `fetched` INTEGER NOT NULL)
                """.trimIndent()
            )
            writableDbDriver.exec(
                """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_PendingArtifact_groupId_artifactId_version` ON 
                        `PendingArtifact` (`groupId`, `artifactId`, `version`)
                """.trimIndent()
            )
            writableDbDriver.exec(
                """
                    CREATE INDEX IF NOT EXISTS
                        `index_PendingArtifact_retries` ON
                        `PendingArtifact` (`retries`)
                """.trimIndent()
            )
        }

        override val endVersion = 1
    }

}