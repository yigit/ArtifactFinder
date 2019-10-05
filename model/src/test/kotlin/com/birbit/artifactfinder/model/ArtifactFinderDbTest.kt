package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.model.db.ArtifactFinderDb
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class ArtifactFinderDbTest {
    private val scope = TestCoroutineScope()
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()
    @Test
    fun checkLikePragmaOff() = scope.runBlockingTest {
        val db = ArtifactFinderDb(null)
        val result = db.query("SELECT 'a' LIKE 'A' AS result") {
            it.nextRow()
            it.requireInt("result")
        }
        Truth.assertThat(result).isEqualTo(0)
    }

    @Test
    fun checkWal() = scope.runBlockingTest {
        val db = ArtifactFinderDb(tmpFolder.newFile().absoluteFile.absolutePath)
        val result = db.query("PRAGMA journal_mode") {
            it.nextRow()
            it.requireString("journal_mode").toLowerCase()
        }
        Truth.assertThat(result).isEqualTo("wal")
    }
}