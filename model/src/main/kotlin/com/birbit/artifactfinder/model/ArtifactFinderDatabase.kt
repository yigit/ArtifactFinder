package com.birbit.artifactfinder.model

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.birbit.androidxsqlitejdbc.inMemoryBuilder
import com.birbit.androidxsqlitejdbc.roomBuilder
import java.util.concurrent.Executor

@Dao
internal abstract class ArtifactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertArtifact(artifact: Artifact): Long

    @Query(
        """
        SELECT * FROM Artifact
        WHERE groupId = :groupId AND artifactId = :artifactId AND version = :version
        LIMIT 1
    """
    )
    abstract suspend fun findArtifact(
        groupId: String,
        artifactId: String,
        version: Version
    ): Artifact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertClassRecord(classRecord: ClassRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertClassLookup(classLookup: ClassLookup)

    @Query("""
        SELECT cr.name, cr.pkg, a.artifactId, a.groupId, a.version
        FROM ClassLookup cl, Artifact a, ClassRecord cr
         WHERE cl.identifier LIKE :query || '%'
            AND cl.classId = cr.id
            AND cr.artifactId = a.id
    """)
    abstract suspend fun search(query: String): List<SearchRecord>

    @Query("SELECT * FROM ClassLookup")
    abstract suspend fun allLookups(): List<ClassLookup>
}

@Database(
    version = 1,
    entities = [
        Artifact::class,
        ClassArtifactAssoc::class,
        ClassLookup::class,
        ClassRecord::class]
)
@TypeConverters(
    Version.RoomTypeConverter::class
)
internal abstract class ArtifactFinderDatabase : RoomDatabase() {
    abstract fun artifactDao(): ArtifactDao

    companion object {
        fun create(name:String? = null,
                   executor : Executor) : ArtifactFinderDatabase {
            val builder = if (name == null) {
                ArtifactFinderDatabase::class.inMemoryBuilder()
            } else {
                ArtifactFinderDatabase::class.roomBuilder(name)
            }
            return builder
                .allowMainThreadQueries()
                .setQueryExecutor(executor)
                .setTransactionExecutor(executor)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA case_sensitive_like=ON;")
                    }
                })
                .build()
        }
    }
}