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
import java.sql.DriverManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class ConnectionProviderTest {

    private val testScope = TestCoroutineScope()

    private val provider = ConnectionProvider(
        limit = 3,
        createConnection = ::createConnection,
        initFirstDb = ::initFirstDb
    )

    @Test
    fun parallelReads() = testScope.runBlockingTest {
        val results = (1..3).map {
            async {
                provider.read {
                    delay(100)
                    now()
                }
            }
        }
        val items = results.awaitAll().toList()
        println(items)
        assertThat(items).isEqualTo(listOf(100, 100, 100))
        assertThat(now()).isEqualTo(100)
    }

    @Test
    fun parallelReads_moreThanCapacity() = testScope.runBlockingTest {
        val results = (1..4).map {
            async {
                provider.read {
                    delay(100)
                    now()
                }
            }
        }
        val res = results.awaitAll().toSet()
        assertThat(res).isEqualTo(setOf(100, 100, 100, 200))
        assertThat(now()).isEqualTo(200)
    }

    @Test
    fun parallelWrites() = testScope.runBlockingTest {
        val results = (1..3).map {
            async {
                provider.write {
                    delay(100)
                    now()
                }
            }
        }
        assertThat(results.awaitAll().toSet()).isEqualTo(setOf(100, 200, 300))
        assertThat(now()).isEqualTo(300)
    }

    @Test
    fun parallelWrites_moreThanCapacity() = testScope.runBlockingTest {
        val results = (1..4).map {
            async {
                provider.write {
                    delay(100)
                    now()
                }
            }
        }
        assertThat(results.awaitAll().toSet()).isEqualTo(setOf(100, 200, 300, 400))
        assertThat(now()).isEqualTo(400)
    }

    @Test
    fun parallelReadsUpgradeToWrites() = testScope.runBlockingTest {
        val results = (1..3).map {
            async {
                provider.read {
                    delay(100)
                    provider.write {
                        delay(5)
                        now()
                    }
                }
            }
        }
        assertThat(results.awaitAll().toSet()).isEqualTo(setOf(105, 110, 115))
        assertThat(now()).isEqualTo(115)
    }

    @Test
    fun parallelReadsUpgradeToWrites_moreThanCapacity() = testScope.runBlockingTest {
        val results = (1..5).map {
            async {
                provider.read {
                    delay(100)
                    provider.write {
                        delay(5)
                        now()
                    }
                }
            }
        }
        assertThat(results.awaitAll().toSet()).isEqualTo(setOf(105, 110, 115, 210, 215))
        // 3 readers: 100 ms
        // 1 writes : 105, another reader comes at 105
        // another writes: 110, another reader comes at 110
        // another writes: 115
        // first available writes, 205 + 5 = 210
        // second available writes, 210 + 5 = 215
        assertThat(now()).isEqualTo(215)
    }

    private fun now() = testScope.currentTime.toInt()

    @Suppress("RedundantSuspendModifier")
    private suspend fun createConnection(): WritableDbDriver {
        return JdbcWriteableDbDriver(DriverManager.getConnection("jdbc:sqlite::memory:"))
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun initFirstDb(driver: WritableDbDriver) {
        ArtifactFinderDb.createAllTables(driver)
    }
}
