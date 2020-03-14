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

package com.birbit.artifactfinder

import com.birbit.artifactfinder.external.ExternalSourceSpec
import com.birbit.artifactfinder.maven.MavenFetcher
import com.birbit.artifactfinder.maven.vo.ArtifactType
import com.birbit.artifactfinder.model.ArtifactFinderModel
import com.birbit.artifactfinder.model.Version
import com.birbit.artifactfinder.parser.Aar
import com.birbit.artifactfinder.parser.CodeSourceParser
import com.birbit.artifactfinder.parser.Jar
import com.birbit.artifactfinder.vo.Artifactory
import com.birbit.artifactfinder.vo.Artifactory.GOOGLE
import com.birbit.artifactfinder.worker.distributeJobs
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

class ArtifactFinder(
    private val db: File
) {
    val model by lazy {
        ArtifactFinderModel(db)
    }

    private val fetchers = Artifactory.values().map {
        MavenFetcher(it)
    }

    private val Artifactory.fetcher
        get() = fetchers[this.ordinal]

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun indexExternalArtifacts() {
        val client = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().also {
                    it.level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
        val response = client.newCall(
            Request.Builder()
                .url(EXTERNAL_SOURCES_URL)
                .build()
        ).execute()
        response.body?.byteStream()?.use {
            val source = ExternalSourceSpec.parse(it)
            if (source.version == ExternalSourceSpec.LATEST_VERSION) {
                indexArtifactSources(source.asArtifactSources())
            }
        }
    }

    suspend fun indexGMaven() = withContext(Dispatchers.IO) {
        val packages = GOOGLE.fetcher.fetchPackages().map {
            GroupSource(
                groupId = it,
                artifactory = GOOGLE
            )
        }
        indexGroupSources(packages)
    }

    private suspend fun indexGroupSources(
        groupSources: List<GroupSource>
    ) {
        val groupIndices = distributeJobs(groupSources) {
            println("fetching group $it")
            it to it.artifactory.fetcher.fetchGroupIndex(it.groupId).also {
                println("done fetching group $it")
            }
        }
        println("DONE getting group indices")
        val groupArtifactPairs = groupIndices.flatMap { groupIndex ->
            groupIndex.second.artifactIds.map { artifactId ->
                ArtifactSource(
                    groupId = groupIndex.second.groupId,
                    artifactId = artifactId,
                    artifactory = groupIndex.first.artifactory,
                    processorCoordinates = null
                )
            }
        }
        println("have artifact-group pairs")
        indexArtifactSources(groupArtifactPairs)
    }

    private suspend fun indexArtifactSources(
        artifactSources: List<ArtifactSource>
    ) {
        distributeJobs(
            items = artifactSources
        ) { artifactSource ->
            println("getting metadata for ${artifactSource.groupId}:${artifactSource.artifactId}")
            val metadata = artifactSource.artifactory.fetcher.fetchArtifactMetadata(
                groupId = artifactSource.groupId,
                artifactId = artifactSource.artifactId
            )
            val selected = VersionSelector.selectVersions(
                metadata.versioning.versions.mapNotNull {
                    Version.fromString(it)
                }
            )
            println("selected $selected")
            selected.forEach {
                println("adding pending $it")
                model.addPendingArtifact(
                    groupId = metadata.groupId,
                    artifactId = metadata.artifactId,
                    version = it,
                    artifactory = artifactSource.artifactory
                )
            }
            println("inserted  ${metadata.groupId} ${metadata.artifactId} to disk")
        }
        model.sync()
    }

    suspend fun fetchArtifacts() {
        distributeJobs(
            items = flow {
                // keep list of dispatched to avoid re-dispatching same items
                val dispatched = mutableListOf<Long>()
                while (true) {
                    val nextArtifact = model.findNextPendingArtifact(dispatched)
                    // increment before distributing to avoid sending the same item again
                    // and again
                    nextArtifact?.let {
                        dispatched.add(it.id)
                        model.incrementPendingArtifactRetry(it)
                    }
                    emit(nextArtifact)
                }
            },
            workers = 5
        ) { pending ->
            println("working on $pending")
            val artifactInfo = pending.artifactory.fetcher.fetchArtifact(
                groupId = pending.groupId,
                artifactId = pending.artifactId,
                version = pending.version.toString()
            )
            val codeSource = when (artifactInfo.type) {
                ArtifactType.AAR -> Aar(artifactInfo.inputStream)
                ArtifactType.JAR -> Jar(artifactInfo.inputStream)
            }
            println("parsing $codeSource for $artifactInfo")
            val parsed = CodeSourceParser.parse(codeSource)
            println("saving $parsed")
            model.saveParsedArtifact(
                pendingArtifact = pending,
                info = parsed
            )
        }
        model.sync()
    }

    companion object {
        private val EXTERNAL_SOURCES_URL = "https://raw.githubusercontent.com/yigit/ArtifactFinder/master/artifactfinder/external_sources.json"
    }
}
