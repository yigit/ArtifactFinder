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

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SharedObjectPoolTest {
    private val scope = TestCoroutineScope()
    private val idCounter = AtomicInteger(0)

    @Test
    fun onCreateOne() = scope.runBlockingTest {
        val pool = SharedObjectPool(10) {
            Item()
        }
        assertThat(pool.use { it }).isEqualTo(Item(1))
        assertThat(pool.use { it }).isEqualTo(Item(1))
        assertThat(pool.createdCnt).isEqualTo(1)
    }

    @Test
    fun limit() = scope.runBlockingTest {
        val pool = SharedObjectPool(7) {
            Item()
        }
        val items = (1..20).map {
            async {
                pool.use {
                    delay(100)
                    it
                }
            }
        }.awaitAll().toSet()
        assertThat(pool.createdCnt).isEqualTo(7)
        assertThat(items).isEqualTo(
            (1..7).map {
                Item(it)
            }.toSet()
        )
        // now ask for more, shouldn't create anymore than needed
        val items2 = (1..7).map {
            async {
                pool.use {
                    delay(100)
                    it
                }
            }
        }.awaitAll().toSet()
        assertThat(pool.createdCnt).isEqualTo(7)
        assertThat(items2).isEqualTo(
            (1..7).map {
                Item(it)
            }.toSet()
        )
    }

    private inner class Item(val id: Int = idCounter.incrementAndGet()) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Item) return false

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id
        }
    }
}
