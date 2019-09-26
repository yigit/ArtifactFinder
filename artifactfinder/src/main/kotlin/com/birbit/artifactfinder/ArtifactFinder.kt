package com.birbit.artifactfinder

import com.birbit.artifactfinder.maven.MavenFetcher
import com.birbit.artifactfinder.maven.vo.ArtifactType
import com.birbit.artifactfinder.maven.vo.Artifactory
import com.birbit.artifactfinder.model.ArtifactFinderModel
import com.birbit.artifactfinder.model.Version
import com.birbit.artifactfinder.parser.Aar
import com.birbit.artifactfinder.parser.CodeSourceParser
import com.birbit.artifactfinder.parser.Jar
import com.birbit.artifactfinder.worker.distributeJobs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

class ArtifactFinder(
    private val db: File
) {
    val model by lazy {
        ArtifactFinderModel(db)
    }

    suspend fun fetchArtifacts() {
        val fetcher = MavenFetcher(
            artifactory = Artifactory.GOOGLE
        )

        distributeJobs(
            items = flow {
                // keep list of dispatched to avoid re-dispatching same items
                val dispatched = mutableListOf<Long>()
                while(true) {
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
            val artifactInfo = fetcher.fetchArtifact(
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

    suspend fun indexGMaven() = withContext(Dispatchers.IO) {
        val fetcher = MavenFetcher(
            artifactory = Artifactory.GOOGLE
        )
        val packages = fetcher.fetchPackages()
        val groupIndices = distributeJobs(packages) {
            println("fetching group $it")
            fetcher.fetchGroupIndex(it).also {
                println("done fetching group $it")
            }
        }
        println("DONE getting group indices")
        val groupArtifactPairs = groupIndices.flatMap { groupIndex ->
            groupIndex.artifactIds.map { artifactId ->
                groupIndex.groupId to artifactId
            }
        }
        println("have paris")
        distributeJobs(
            items = groupArtifactPairs
        ) {
            println("getting metadata for ${it.first}:${it.second}")
            val metadata = fetcher.fetchArtifactMetadata(
                groupId = it.first,
                artifactId = it.second
            )
            val selected = VersionSelector.selectVersions(metadata.versioning.versions.mapNotNull {
                Version.fromString(it)
            })
            println("selected $selected")
            selected.forEach {
                println("adding pending $it")
                model.addPendingArtifact(
                    groupId = metadata.groupId,
                    artifactId = metadata.artifactId,
                    version = it
                )
            }
            println("inserted  ${metadata.groupId} ${metadata.artifactId} to disk")
        }
        model.sync()
    }
}