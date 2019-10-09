package com.birbit.artifactfinder.maven

import com.birbit.artifactfinder.vo.Artifactory
import kotlinx.coroutines.runBlocking

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