package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.model.db.ArtifactFinderDb
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class ArtifactFinderDbTest {
    private val scope = TestCoroutineScope()
    @Test
    fun checkLikePragmaOff() = scope.runBlockingTest {
        val db = ArtifactFinderDb(null)
        val result = db.query("SELECT 'a' LIKE 'A' AS result") {
            it.nextRow()
            it.getInt("result")
        }
        Truth.assertThat(result).isEqualTo(0)
    }
}