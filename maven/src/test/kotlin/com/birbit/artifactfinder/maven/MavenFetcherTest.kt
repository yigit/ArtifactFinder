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

package com.birbit.artifactfinder.maven

import com.birbit.artifactfinder.maven.vo.ArtifactMetadata
import com.birbit.artifactfinder.maven.vo.GroupIndex
import com.birbit.artifactfinder.maven.vo.Versioning
import com.birbit.artifactfinder.vo.Artifactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MavenFetcherTest {
    private val resourceDispatcher = object : okhttp3.mockwebserver.Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            fun loadResource(name: String): String {
                return MavenFetcherTest::class.java.getResourceAsStream("/response/$name")
                    .source()
                    .buffer()
                    .readUtf8()
            }
            return when (request.path!!.substring(1)) {
                MavenApi.MASTER_INDEX -> MockResponse()
                    .setResponseCode(200)
                    .setBody(loadResource("master-index.xml"))
                "foo/bar/${MavenApi.GROUP_INDEX}" -> MockResponse()
                    .setResponseCode(200)
                    .setBody(loadResource("group-index.xml"))
                "foo/bar/artifact1/${MavenApi.MAVEN_METADATA}" -> MockResponse()
                    .setResponseCode(200)
                    .setBody(loadResource("maven-metadata.xml"))
                "foo/bar/artifact1/2.2.0-rc01/artifact1-2.2.0-rc01.jar" -> MockResponse()
                    .setResponseCode(200)
                    .setBody("jarContent")
                "foo/bar/artifact-2/2.2.0-rc01/artifact-2-2.2.0-rc01.aar" -> MockResponse()
                    .setResponseCode(200)
                    .setBody("aarContent")
                else -> MockResponse()
                    .setResponseCode(404)
                    .setBody("what is ${request.path}")
            }
        }
    }

    val mockServer = MockWebServer().also {
        it.dispatcher = resourceDispatcher
    }
    val fetcher = MavenFetcher(
        artifactory = Artifactory.GOOGLE,
        baseUrl = mockServer.url("/")
    )

    @Test
    fun fetchMasterIndex() = runBlocking<Unit> {
        val masterIndex = fetcher.fetchPackages()
        assertThat(masterIndex).containsExactly(
            "androidx.foo.bar",
            "androidx.baz.bar"
        )
    }

    @Test
    fun fetchGroupIndex() = runBlocking<Unit> {
        val groupIndex = fetcher.fetchGroupIndex("foo.bar")
        assertThat(groupIndex).isEqualTo(
            GroupIndex(
                groupId = "foo.bar",
                artifactIds = setOf("artifact1", "artifact-2")
            )
        )
    }

    @Test
    fun fetchArtifactMetadata() = runBlocking {
        val metadata = fetcher.fetchArtifactMetadata("foo.bar", "artifact1")
        assertThat(metadata).isEqualTo(
            ArtifactMetadata(
                groupId = "foo.bar",
                artifactId = "artifact1",
                versioning = Versioning(
                    latest = "2.2.0-rc01",
                    release = "2.2.0-rc01",
                    versions = setOf(
                        "2.0.0-alpha1", "2.0.0-beta01", "2.0.0-rc01", "2.0.0", "2.1.0-alpha01"
                    )
                )
            )
        )
    }

    @Test
    fun fetchArtifact_jar() = runBlocking {
        val jar = fetcher.fetchArtifact(
            groupId = "foo.bar",
            artifactId = "artifact1",
            version = "2.2.0-rc01"
        )
        assertThat(jar.inputStream.source().buffer().readUtf8()).isEqualTo("jarContent")
    }

    @Test
    fun fetchArtifact_aar() = runBlocking {
        val aar = fetcher.fetchArtifact(
            groupId = "foo.bar",
            artifactId = "artifact-2",
            version = "2.2.0-rc01"
        )
        assertThat(aar.inputStream.source().buffer().readUtf8()).isEqualTo("aarContent")
    }
}
