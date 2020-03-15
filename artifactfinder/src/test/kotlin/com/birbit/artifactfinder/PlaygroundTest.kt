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

import com.birbit.artifactfinder.maven.MavenFetcher
import com.birbit.artifactfinder.maven.vo.ArtifactType
import com.birbit.artifactfinder.model.ArtifactFinderModel
import com.birbit.artifactfinder.parser.Aar
import com.birbit.artifactfinder.parser.CodeSourceParser
import com.birbit.artifactfinder.parser.Jar
import com.birbit.artifactfinder.vo.Artifactory
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PlaygroundTest {

    @Test
    fun createDb() = runBlocking<Unit> {
        if (true) return@runBlocking
        val targetFile = File("/home/yboyar/src/ArtifactFinder/tmp/artifacts.db")
        targetFile.delete()
        val model = ArtifactFinderModel(targetFile)
        model.findNextPendingArtifact(emptyList())
    }

    @Test
    fun playground() = runBlocking<Unit> {
        if (true) return@runBlocking
        val fetcher = MavenFetcher(
            artifactory = Artifactory.MAVEN
        )
        val artifactInfo = fetcher.fetchArtifact(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.3.4"
        )
        val codeSource = when (artifactInfo.type) {
            ArtifactType.AAR -> Aar(artifactInfo.inputStream)
            ArtifactType.JAR -> Jar(artifactInfo.inputStream)
        }
        val src = CodeSourceParser.parse(codeSource)
        println(src)
    }
}
