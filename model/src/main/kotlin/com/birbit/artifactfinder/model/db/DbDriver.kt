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

import java.io.Closeable

interface QueryResult : Closeable {
    val columnNames: Set<String>
    fun hasColumn(name: String) = columnNames.contains(name)
    fun nextRow(): Boolean
    fun requireInt(columnName: String): Int
    fun requireLong(columnName: String): Long
    fun requireString(columnName: String): String
    fun getString(columnName: String): String?
    fun requireBoolean(columnName: String): Boolean = requireInt(columnName) == 1
    fun asSequence() = generateSequence {
        if (nextRow()) {
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
    suspend fun <T> query(block: (QueryResult) -> T): T
}

interface WriteQuery : Query {
    suspend fun exec(): Boolean
    suspend fun <T> execForLastRowId(block: (Long) -> T): T
    suspend fun execForLastRowId(): Long = execForLastRowId { it }
}

interface DbDriver {
    fun prepareRead(sql: String): Query
}

interface WritableDbDriver : DbDriver {
    suspend fun exec(sql: String) = prepareWrite(sql).exec()
    fun prepareWrite(sql: String): WriteQuery
    suspend fun <T> withTransaction(block: suspend () -> T): T
}
