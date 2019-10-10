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

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
