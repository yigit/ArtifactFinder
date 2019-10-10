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

package com.birbit.artifactfinder.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

suspend fun <T : Any, R> distributeJobs(
    items: List<T>,
    workers: Int = (items.size / 5).coerceAtMost(5).coerceAtLeast(1),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    consumer: suspend (T) -> R
): List<R> = distributeJobs(
    items = flow {
        items.forEach {
            emit(it)
        }
    },
    workers = workers,
    dispatcher = dispatcher,
    consumer = consumer
)

private fun log(msg: Any?) {
    println("[JQ][${Thread.currentThread().name}]: $msg")
}

suspend fun <T : Any, R> distributeJobs(
    items: Flow<T?>,
    workers: Int,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    consumer: suspend (T) -> R
): List<R> = withContext(dispatcher) {
    val distributionChannel = Channel<T>()
    launch {
        try {
            items.collect {
                if (it != null) {
                    log("dispatch $it")
                    distributionChannel.send(it)
                } else {
                    log("flow emitted null")
                    throw AbortFlowException()
                }
            }
        } catch (abort: AbortFlowException) {
        } finally {
            distributionChannel.close()
            log("distributionChannel DONE")
        }
    }
    val resultLock = Mutex()
    val results = mutableListOf<R>()
    (0 until workers).map { workerId ->
        launch {
            while (!distributionChannel.isClosedForReceive) {
                try {
                    val value = distributionChannel.receive()
                    log("will process $value in $workerId")
                    val result = consumer(value)
                    log("will save result $result for $workerId")
                    resultLock.withLock {
                        log("saving result for $workerId")
                        results.add(result)
                    }
                } catch (closed: ClosedReceiveChannelException) {
                    log("worker $workerId done")
                } catch (th: Throwable) {
                    log("worker has thrown exception ${th.message}")
                }
            }
        }
    }.joinAll()
    log("JobQueue DONE")
    return@withContext results
}

private class AbortFlowException : CancellationException()
