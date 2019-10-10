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
