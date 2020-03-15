/*
 * Copyright 2020 Google, Inc.
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

package com.birbit.artifactfinder.external

import com.birbit.artifactfinder.maven.MavenFetcher
import com.birbit.artifactfinder.vo.Artifactory
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ExternalSourceSpecTest {
    @Test
    fun testEmpty() {
        assertThat("""{}""".parse()).isEqualTo(
            ExternalSourceSpec(
                version = 1,
                groups = emptyList()
            )
        )
    }

    @Test
    fun test_oneArtifact() {
        assertThat(
            """{
            "groups" : [
                {
                    "groupId" : "foo.bar",
                    "artifactIds" : ["baz"]
                }
            ]
            }""".trimMargin().parse()
        ).isEqualTo(
            ExternalSourceSpec(
                version = 1,
                groups = listOf(
                    ExternalGroupSpec(
                        groupId = "foo.bar",
                        artifactIds = listOf("baz")
                    )
                )
            )
        )
    }

    @Test
    fun test_ignoreUnknown() {
        assertThat(
            """{
            "ignore_unknown" : true,
            "groups" : [
                {
                    "groupId" : "foo.bar",
                    "artifactIds" : ["baz"]
                }
            ]
            }""".trimMargin().parse()
        ).isEqualTo(
            ExternalSourceSpec(
                version = 1,
                groups = listOf(
                    ExternalGroupSpec(
                        groupId = "foo.bar",
                        artifactIds = listOf("baz")
                    )
                )
            )
        )
    }

    @Test
    fun test_emptyArtifact() {
        val result = runCatching {
            """{
            "groups" : [
                {
                    "groupId" : "foo.bar"
                }
            ]
            }""".trimMargin().parse()
        }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().contains(
            "Artifacts for a package cannot be empty"
        )
    }

    @Test
    fun validateCurrent() = runBlocking<Unit> {
        val current = ExternalSourceSpecTest::class.java.getResourceAsStream("/external_sources.json")
            .reader(Charsets.UTF_8)
            .readText().parse()
        assertThat(current.groups).isNotEmpty()
        val fetchers = mutableMapOf<Artifactory, MavenFetcher>()
        current.asArtifactSources().forEach {
            val fetcher = fetchers.getOrPut(it.artifactory) {
                MavenFetcher(it.artifactory)
            }
            val mavenInfo = runCatching {
                fetcher.fetchArtifactMetadata(it.groupId, it.artifactId)
            }
            assertWithMessage("should be able to fetch ${it.groupId} / ${it.artifactId}")
                .that(mavenInfo.isSuccess).isTrue()
        }
    }

    private fun String.parse(): ExternalSourceSpec {
        return ExternalSourceSpec.parse(this)
    }
}
