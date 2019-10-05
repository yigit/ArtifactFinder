package com.birbit.artifactfinder.model.db

import java.io.Closeable

interface QueryResult : Closeable {
    fun nextRow(): Boolean
    fun getInt(columnName: String): Int
    fun getLong(columnName: String): Long
    fun getString(columnName: String): String
    fun getBoolean(columnName: String): Boolean = getInt(columnName) == 1
    fun asSequence() = generateSequence {
        if(nextRow()) {
            this@QueryResult
        } else {
            close()
            null
        }

    }
}

interface Query {
    fun bindInt(index: Int, value: Int)
    fun bindLong(index: Int, value: Long)
    fun bindString(index: Int, value: String)
    fun bindNull(index: Int)
    fun bindBoolean(index: Int, value: Boolean) = bindInt(index, if (value) 1 else 0)
    suspend fun <T> query(block : (QueryResult) -> T): T
}

interface WriteQuery : Query {
    suspend fun exec(): Boolean
    suspend fun <T> execForLastRowId(block: (Long) -> T): T
    suspend fun execForLastRowId(): Long = execForLastRowId { it }
}

interface DbDriver {
    fun prepareRead(sql:String) : Query
}

interface WritableDbDriver : DbDriver {
    suspend fun exec(sql: String) =  prepareWrite(sql).exec()
    fun prepareWrite(sql:String) : WriteQuery
    suspend fun <T> withTransaction(block: suspend () ->T) : T
}