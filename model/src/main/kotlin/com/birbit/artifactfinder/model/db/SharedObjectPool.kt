package com.birbit.artifactfinder.model.db

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedObjectPool<T>(
    private val limit: Int,
    private val creator: suspend () -> T
) {
    init {
        check(limit > 0) {
            "shared object pool limit cannot be less than 1, c'mon"
        }
    }

    val createdCnt
        get() = counter
    private val free = Channel<T>(capacity = Channel.UNLIMITED)
    private val lock = Mutex()
    private var counter = 0
    private suspend fun acquire(): T {
        val existing = free.poll()
        if (existing != null) {
            return existing
        }
        // nothing ready, trigger a create and then wait forever
        val create = lock.withLock {
            if (counter < limit) {
                counter++
                true
            } else {
                false
            }
        }
        if (create) {
            free.send(creator())
        }

        // now wait forever
        return free.receive()
    }

    private suspend fun release(t: T) {
        free.send(t)
    }

    suspend fun <R> use(
        block: suspend (T) -> R
    ): R {
        val item = acquire()
        return try {
            block(item)
        } finally {
            release(item)
        }
    }
}