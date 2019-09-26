package com.birbit.artifactfinder.maven

import com.birbit.artifactfinder.maven.vo.Artifactory
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

//@RunWith(JUnit4::class)
class RealApiTest {
  //  @Test
    fun realTest() = runBlocking{
        val fetcher = MavenFetcher(
            artifactory = Artifactory.GOOGLE
        )
        val result = fetcher.fetchArtifact(
            groupId = "androidx.test",
            artifactId = "orchestrator",
            version = "1.1.0"
        )
        println("result: $result")
    }
}