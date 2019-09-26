package com.birbit.artifactfinder.model

import androidx.room.RoomDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

internal class DatabaseProvider<T : RoomDatabase>(
    private val db : T
) {

    private val dbMutex = Mutex()

    suspend fun <R> use(
        block : suspend T.() -> R
    ) : R {
        @Suppress("UNCHECKED_CAST")
        val owner = coroutineContext[DbOwner.KEY]
        return if (owner == null) {
            dbMutex.withLock {
                val newOwner = DbOwner()
                withContext(coroutineContext + newOwner) {
                    block(db)
                }
            }
        } else {
            // owns the db
            block(db)
        }
    }


    internal class DbOwner() : AbstractCoroutineContextElement(KEY) {
        companion object {
            internal val KEY = object :  CoroutineContext.Key<DbOwner>{}
        }
    }
}