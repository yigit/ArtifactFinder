package com.birbit.artifactfinder.model.db

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

internal class JdbcQueryResult(
    private val rs: ResultSet
) : QueryResult {
    override val columnNames: Set<String> = (1..rs.metaData.columnCount).map {
        rs.metaData.getColumnName(it)
    }.toSet()

    override fun nextRow() = rs.next()

    override fun requireInt(columnName: String) = rs.getInt(columnName)

    override fun requireLong(columnName: String): Long = rs.getLong(columnName)

    override fun requireString(columnName: String): String = rs.getString(columnName)

    override fun getString(columnName: String): String? = rs.getString(columnName)

    override fun close() {
        rs.close()
    }

}

internal open class JdbcQueryImpl(
    val stmt: PreparedStatement
) : Query {
    override fun bindInt(index: Int, value: Int) {
        stmt.setInt(index, value)
    }

    override fun bindLong(index: Int, value: Long) {
        stmt.setLong(index, value)
    }

    override fun bindString(index: Int, value: String) {
        stmt.setString(index, value)
    }

    override fun bindNull(index: Int) {
        stmt.setNull(index, Types.NULL)
    }

    override suspend fun <T> query(block: (QueryResult) -> T): T {
        val rs = JdbcQueryResult(stmt.executeQuery())
        return rs.use { result ->
            block(result)
        }
    }
}

internal class JdbcWritableQueryImpl(
    stmt: PreparedStatement,
    private val conn: Connection
) : JdbcQueryImpl(stmt), WriteQuery {
    override suspend fun exec() = stmt.execute()

    override suspend fun <T> execForLastRowId(block: (Long) -> T): T {
        stmt.execute()
        return conn.prepareStatement("SELECT last_insert_rowid()").executeQuery().use {
            block(it.getLong(1))
        }
    }

}

internal open class JdbcDbDriver(
    val conn: Connection
) : DbDriver {
    override fun prepareRead(sql: String): Query {
        return JdbcQueryImpl(conn.prepareStatement(sql))
    }
}

internal class JdbcWriteableDbDriver(
    conn: Connection
) : JdbcDbDriver(conn), WritableDbDriver {
    override suspend fun <T> withTransaction(block: suspend () -> T): T {
        conn.autoCommit = false
        try {
            val result = block()
            conn.commit()
            return result
        } finally {
            conn.autoCommit = true
        }
    }

    override fun prepareWrite(sql: String): WriteQuery {
        return JdbcWritableQueryImpl(
            stmt = conn.prepareStatement(sql),
            conn = conn
        )
    }

    override fun prepareRead(sql: String): Query {
        return JdbcQueryImpl(conn.prepareStatement(sql))
    }
}