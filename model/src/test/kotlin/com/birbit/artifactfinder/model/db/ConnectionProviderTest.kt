package com.birbit.artifactfinder.model.db

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.sql.DriverManager

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
        val results = (1..4).map { cnt ->
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