package com.birbit.artifactfinder.model.db

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class ConnectionProvider(
    limit: Int,
    private val createConnection: suspend () -> WritableDbDriver,
    private val initFirstDb: suspend (WritableDbDriver) -> Unit
) {
    private val pool = SharedObjectPool(
        limit = limit,
        creator = ::doCreate
    )

    private val writeLock = Mutex()

    private val createLock = Mutex()
    private var first = true
    private suspend fun doCreate(): WritableDbDriver {
        val conn = createConnection()
        createLock.withLock {
            if (first) {
                first = false
                initFirstDb(conn)
            }
        }
        return conn
    }

    private suspend fun <R> use(block: suspend WritableDbDriver.() -> R): R {
        @Suppress("UNCHECKED_CAST")
        val contextElm = coroutineContext[ContextConnection.KEY]
        if (contextElm != null) {
            return contextElm.conn.block()
        }
        return pool.use { myConn ->
            val newConn = ContextConnection(myConn, false)
            try {
                withContext(coroutineContext + newConn) {
                    myConn.block()
                }
            } finally {
                newConn.releaseIfWriteable(writeLock)
            }
        }
    }

    suspend fun <R> write(
        block: suspend WritableDbDriver.() -> R
    ): R {
        return use {
            val contextElm = checkNotNull(coroutineContext[ContextConnection.KEY])
            contextElm.makeWriteable(writeLock)
            block()
        }
    }

    suspend fun <R> read(
        block: suspend DbDriver.() -> R
    ): R {
        return use {
            block()
        }
    }

    internal class ContextConnection(
        val conn: WritableDbDriver,
        @Volatile var writeable: Boolean
    ) : AbstractCoroutineContextElement(KEY) {
        companion object {
            internal val KEY = object : CoroutineContext.Key<ContextConnection> {}
        }

        suspend fun makeWriteable(mutex: Mutex) {
            if (writeable) return
            mutex.lock(this)
            writeable = true
        }

        suspend fun releaseIfWriteable(mutex: Mutex) {
            if (!writeable) return
            writeable = false
            mutex.unlock(this)
        }
    }
}