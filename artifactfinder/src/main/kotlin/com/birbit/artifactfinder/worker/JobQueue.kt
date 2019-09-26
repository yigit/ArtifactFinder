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

private fun log(msg:Any?) {
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
        } catch (abort : AbortFlowException) {

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
                }
            }
        }
    }.joinAll()
    log("JobQueue DONE")
    return@withContext results
}

private class AbortFlowException : CancellationException()