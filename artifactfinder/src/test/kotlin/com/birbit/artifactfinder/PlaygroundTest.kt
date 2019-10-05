package com.birbit.artifactfinder

import com.birbit.artifactfinder.maven.MavenFetcher
import com.birbit.artifactfinder.maven.vo.ArtifactType
import com.birbit.artifactfinder.maven.vo.Artifactory
import com.birbit.artifactfinder.model.ArtifactFinderModel
import com.birbit.artifactfinder.parser.Aar
import com.birbit.artifactfinder.parser.CodeSourceParser
import com.birbit.artifactfinder.parser.Jar
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

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
            artifactory = Artifactory.GOOGLE
        )
        val artifactInfo = fetcher.fetchArtifact(
            groupId = "androidx.fragment",
            artifactId = "fragment-ktx",
            version = "1.2.0-alpha04"
        )
        val codeSource = when (artifactInfo.type) {
            ArtifactType.AAR -> Aar(artifactInfo.inputStream)
            ArtifactType.JAR -> Jar(artifactInfo.inputStream)
        }
        val src = CodeSourceParser.parse(codeSource)
        println(src)
    }
}