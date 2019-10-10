/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.birbit.artifactfinder.model.db

import com.birbit.artifactfinder.model.*
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.ARTIFACT
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.ARTIFACTORY_ID
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.ARTIFACT_ID
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.ID
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.IDENTIFIER
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.METHOD_ID
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.METHOD_LOOKUP
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.METHOD_RECORD
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.NAME
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.PENDING_ARTIFACT
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.PKG
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.RECEIVER_NAME
import com.birbit.artifactfinder.model.db.ArtifactDaoImpl.Companion.RECEIVER_PKG
import com.birbit.artifactfinder.vo.Artifactory
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
                INSERT OR REPLACE INTO $ARTIFACT
                    (`$ID`,`$GROUP_ID`,`$ARTIFACT_ID`,`$VERSION`, `$ARTIFACTORY_ID`)
                    VALUES (nullif(?, 0),?,?,?,?)
            """.trimIndent()
            ).also {
                it.bindLong(1, artifact.id)
                it.bindString(2, artifact.groupId)
                it.bindString(3, artifact.artifactId)
                it.bindString(4, artifact.version.toString())
                it.bindInt(5, artifact.artifactory.id)
            }.execForLastRowId()
        }
    }

    override suspend fun findArtifact(groupId: String, artifactId: String, version: Version): Artifact? {
        return conn.read {
            prepareRead(
                """
                SELECT * FROM $ARTIFACT
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
                INSERT OR REPLACE INTO $CLASS_RECORD
                    (`$ID`,`$PKG`,`$NAME`,`$ARTIFACT_ID`)
                    VALUES (nullif(?, 0),?,?,?)
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
                    INSERT OR REPLACE INTO $CLASS_LOOKUP
                        (`$IDENTIFIER`,`$CLASS_ID`)
                        VALUES (?,?)
                """.trimIndent()
            ).also { query ->
                query.bindString(1, classLookup.identifier)
                query.bindLong(2, classLookup.classId)
            }.exec()
        }
    }

    override suspend fun searchClasses(query: String): List<SearchRecord> {
        return conn.read {
            prepareRead(
                """
                SELECT cr.$NAME, cr.$PKG, a.$ARTIFACT_ID, a.$GROUP_ID, a.$VERSION
                    FROM $CLASS_LOOKUP cl, $ARTIFACT a, $CLASS_RECORD cr
                    WHERE cl.$IDENTIFIER LIKE ? || '%'
                        AND cl.$CLASS_ID = cr.$ID
                        AND cr.$ARTIFACT_ID = a.$ID
                """.trimIndent()
            ).also {
                it.bindString(1, query)
            }.query { result ->
                result.asSearchRecords(SearchRecord.Type.CLASS)
            }
        }
    }

    override suspend fun searchMethods(
        query: String,
        methodSearchType: ArtifactDao.MethodSearchType
    ): List<SearchRecord> {
        val methodConstraint = when (methodSearchType) {
            ArtifactDao.MethodSearchType.ALL_METHOD -> "1"
            ArtifactDao.MethodSearchType.ONLY_EXTENSIONS -> "mr.$RECEIVER_NAME IS NOT NULL"
            ArtifactDao.MethodSearchType.ONLY_GLOBAL -> "mr.$RECEIVER_NAME IS NULL"
        }
        return conn.read {
            prepareRead(
                """
                SELECT mr.$NAME, mr.$PKG, mr.$RECEIVER_NAME, a.$ARTIFACT_ID, a.$GROUP_ID, a.$VERSION
                    FROM $METHOD_LOOKUP ml, $ARTIFACT a, $METHOD_RECORD mr
                    WHERE ml.$IDENTIFIER LIKE ? || '%'
                        AND $methodConstraint
                        AND ml.$METHOD_ID = mr.$ID
                        AND mr.$ARTIFACT_ID = a.$ID
                """.trimIndent()
            ).also {
                it.bindString(1, query)
            }.query { result ->
                result.asSearchRecords(null)
            }
        }
    }

    override suspend fun allLookups(): List<ClassLookup> {
        return conn.read {
            prepareRead("SELECT * FROM $CLASS_LOOKUP").query {
                it.asClassLookups()
            }
        }
    }

    override suspend fun insertPendingArtifact(pendingArtifact: PendingArtifact) {
        conn.write {
            prepareWrite(
                """
                INSERT OR IGNORE INTO $PENDING_ARTIFACT
                    (`$ID`,`$GROUP_ID`,`$ARTIFACT_ID`,`$VERSION`,`$RETRIES`,`$FETCHED`, `$ARTIFACTORY_ID`)
                    VALUES (nullif(?, 0),?,?,?,?,?, ?)
            """.trimIndent()
            ).also {
                it.bindLong(1, pendingArtifact.id)
                it.bindString(2, pendingArtifact.groupId)
                it.bindString(3, pendingArtifact.artifactId)
                it.bindString(4, pendingArtifact.version.toString())
                it.bindInt(5, pendingArtifact.retries)
                it.bindBoolean(6, pendingArtifact.fetched)
                it.bindInt(7, pendingArtifact.artifactory.id)
            }.exec()
        }
    }

    override suspend fun findPendingArtifact(groupId: String, artifactId: String, version: Version): Artifact? {
        return conn.read {
            prepareRead(
                """
                SELECT * FROM $PENDING_ARTIFACT
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
                UPDATE $PENDING_ARTIFACT
                SET $RETRIES = $RETRIES + 1
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
                UPDATE $PENDING_ARTIFACT
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
                SELECT * FROM $PENDING_ARTIFACT
                WHERE $FETCHED = 0 AND $RETRIES < 20 AND $ID NOT IN($idsParam)
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

    override suspend fun insertMethodRecord(methodRecord: MethodRecord): Long {
        return conn.write {
            prepareWrite(
                """
                INSERT INTO $METHOD_RECORD ($ID, $PKG, $NAME, $RECEIVER_PKG, $RECEIVER_NAME, $ARTIFACT_ID)
                VALUES (nullif(?, 0),?,?,?,?,?)
            """.trimIndent()
            ).also {
                it.bindLong(1, methodRecord.id)
                it.bindString(2, methodRecord.pkg)
                it.bindString(3, methodRecord.name)
                if (methodRecord.receivePkg != null) {
                    it.bindString(4, methodRecord.receivePkg)
                } else {
                    it.bindNull(4)
                }
                if (methodRecord.receiveName != null) {
                    it.bindString(5, methodRecord.receiveName)
                } else {
                    it.bindNull(5)
                }
                it.bindLong(6, methodRecord.artifactId)
            }.execForLastRowId()
        }
    }

    override suspend fun insertMethodLookup(methodLookup: MethodLookup) {
        conn.write {
            prepareWrite(
                """
                    INSERT OR REPLACE INTO $METHOD_LOOKUP
                        (`$IDENTIFIER`,`$METHOD_ID`)
                        VALUES (?,?)
                """.trimIndent()
            ).also { query ->
                query.bindString(1, methodLookup.identifier)
                query.bindLong(2, methodLookup.methodId)
            }.exec()
        }
    }

    private fun QueryResult.asArtifact() = Artifact(
        id = requireLong(ID),
        groupId = requireString(GROUP_ID),
        artifactId = requireString(ARTIFACT_ID),
        version = Version.fromString(requireString(VERSION))!!,
        artifactory = Artifactory.getById(requireInt(ARTIFACTORY_ID))
    )

    private fun QueryResult.asSearchRecords(type: SearchRecord.Type?): List<SearchRecord> {
        val mightHaveReceiver = hasColumn(RECEIVER_NAME)
        return asSequence().map {
            val decidedType = type ?: if (mightHaveReceiver && it.getString(RECEIVER_NAME) != null) {
                SearchRecord.Type.EXTENSION_METHOD
            } else {
                SearchRecord.Type.GLOBAL_METHOD
            }
            SearchRecord(
                pkg = requireString(PKG),
                name = requireString(NAME),
                type = decidedType,
                receiverName = if (mightHaveReceiver) {
                    getString(RECEIVER_NAME)
                } else {
                    null
                },
                groupId = requireString(GROUP_ID),
                artifactId = requireString(ARTIFACT_ID),
                version = Version.fromString(requireString(VERSION))!!
            )
        }.toList()
    }

    private fun QueryResult.asClassLookups() = asSequence().map {
        ClassLookup(
            identifier = requireString(IDENTIFIER),
            classId = requireLong(CLASS_ID)
        )
    }.toList()

    private fun QueryResult.asPendingArtifact() = PendingArtifact(
        id = requireLong(ID),
        groupId = requireString(GROUP_ID),
        artifactId = requireString(ARTIFACT_ID),
        version = Version.fromString(requireString(VERSION))!!,
        retries = requireInt(RETRIES),
        fetched = requireBoolean(FETCHED),
        artifactory = Artifactory.getById(requireInt(ARTIFACTORY_ID))
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
        internal const val ARTIFACTORY_ID = "artifactoryId"
        internal const val GROUP_ID = "groupId"
        internal const val VERSION = "version"
        internal const val PKG = "pkg"
        internal const val NAME = "name"
        internal const val IDENTIFIER = "identifier"
        internal const val CLASS_ID = "classId"
        internal const val RETRIES = "retries"
        internal const val FETCHED = "fetched"
        internal const val METHOD_RECORD = "MethodRecord"
        internal const val METHOD_ID = "methodId"
        internal const val RECEIVER_PKG = "receiverPkg"
        internal const val RECEIVER_NAME = "receiverName"
        internal const val METHOD_LOOKUP = "MethodLookup"
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
                val migrations = listOf(Migration_1(), Migration_2())
                val version = writableDbDriver.prepareRead(
                    """
                    PRAGMA user_version
                """.trimIndent()
                ).query {
                    if (it.nextRow()) {
                        it.requireInt("user_version")
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
                    writableDbDriver.exec(
                        """
                        PRAGMA user_version = ${selected.last().endVersion}
                        """.trimIndent()
                    )
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
                    CREATE TABLE IF NOT EXISTS $METHOD_RECORD (
                        $ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        $NAME TEXT NOT NULL,
                        $PKG TEXT NOT NULL,
                        $RECEIVER_PKG TEXT, -- nullable for global methods
                        $RECEIVER_NAME TEXT, -- nullable for global methods
                        $ARTIFACT_ID INTEGER NOT NULL REFERENCES $ARTIFACT($ID) ON DELETE CASCADE ON UPDATE CASCADE DEFERRABLE
                    )
                """.trimIndent()
            )
            writableDbDriver.exec(
                """
                    CREATE TABLE IF NOT EXISTS $METHOD_LOOKUP (
                        $IDENTIFIER TEXT NOT NULL,
                        $METHOD_ID INTEGER NOT NULL REFERENCES $METHOD_RECORD($ID) ON DELETE CASCADE ON UPDATE CASCADE,
                        PRIMARY KEY(`$IDENTIFIER`, `$METHOD_ID`)
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

    internal class Migration_2 : Migration {
        override val endVersion = 2

        override suspend fun apply(writableDbDriver: WritableDbDriver) {
            writableDbDriver.exec(
                """
                ALTER TABLE $ARTIFACT ADD COLUMN $ARTIFACTORY_ID INTEGER DEFAULT ${Artifactory.GOOGLE.id}
            """.trimIndent()
            )
            writableDbDriver.exec(
                """
                ALTER TABLE $PENDING_ARTIFACT ADD COLUMN $ARTIFACTORY_ID INTEGER DEFAULT ${Artifactory.GOOGLE.id}
            """.trimIndent()
            )
            writableDbDriver.exec(
                """
                UPDATE $ARTIFACT SET $ARTIFACTORY_ID = ${Artifactory.GOOGLE.id}
            """.trimIndent()
            )
            writableDbDriver.exec(
                """
                UPDATE $PENDING_ARTIFACT SET $ARTIFACTORY_ID = ${Artifactory.GOOGLE.id}
            """.trimIndent()
            )
        }
    }
}
