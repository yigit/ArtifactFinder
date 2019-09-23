package com.birbit.artifactfinder.model

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors

@RunWith(JUnit4::class)
class ArtifactFinderDbTest {
    @Test
    fun checkLikePragmaOff() {
        val db = ArtifactFinderDatabase.create(
            name = null,
            executor = Executors.newSingleThreadExecutor())
        try {
            db.query("SELECT 'a' LIKE 'A'", emptyArray()).use {
                it.moveToFirst()
                Truth.assertThat(it.getInt(0)).isEqualTo(0)
            }
        } finally {
            db.close()
        }
    }
}