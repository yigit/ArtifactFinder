package com.birbit.artifactfinder

import com.birbit.artifactfinder.maven.MavenFetcher
import com.birbit.artifactfinder.maven.vo.ArtifactType
import com.birbit.artifactfinder.maven.vo.Artifactory
import com.birbit.artifactfinder.parser.Aar
import com.birbit.artifactfinder.parser.CodeSourceParser
import com.birbit.artifactfinder.parser.Jar
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PlaygroundTest {
    @Test
    fun playground() = runBlocking<Unit> {
        if (true) return@runBlocking
        val fetcher = MavenFetcher(
            artifactory = Artifactory.GOOGLE
        )
        val artifactInfo = fetcher.fetchArtifact(
            groupId = "com.google.firebase",
            artifactId = "firebase-inappmessaging-display",
            version = "19.0.1"
        )
        val codeSource = when (artifactInfo.type) {
            ArtifactType.AAR -> Aar(artifactInfo.inputStream)
            ArtifactType.JAR -> Jar(artifactInfo.inputStream)
        }
        val src = CodeSourceParser.parse(codeSource)
        println(src)
    }
}