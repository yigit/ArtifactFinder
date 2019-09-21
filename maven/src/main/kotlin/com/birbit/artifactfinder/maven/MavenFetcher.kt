package com.birbit.artifactfinder.maven

import com.birbit.artifactfinder.maven.vo.Artifact
import com.birbit.artifactfinder.maven.vo.ArtifactMetadata
import com.birbit.artifactfinder.maven.vo.ArtifactType
import com.birbit.artifactfinder.maven.vo.Artifactory
import com.birbit.artifactfinder.maven.vo.GroupIndex
import okhttp3.HttpUrl
import retrofit2.HttpException

class MavenFetcher internal constructor(
    artifactory: Artifactory,
    baseUrl: HttpUrl? = null
) {

    constructor(artifactory: Artifactory) : this(artifactory, null)

    private val mavenApi = MavenApi.create(
        artifactory = artifactory,
        baseUrl = baseUrl
    )

    suspend fun fetchPackages() = mavenApi.masterIndex().keys
        .filter(::shouldParsePackage)

    suspend fun fetchGroupIndex(groupId: String) = mavenApi.groupIndex(
        groupId.toPath()
    ).let {
        GroupIndex(
            groupId = groupId,
            artifactIds = it.keys.filter {
                shouldParseArtfiact(groupId, it)
            }.toSet()
        )
    }

    suspend fun fetchArtifactMetadata(groupId: String, artifactId: String): ArtifactMetadata {
        return mavenApi.mavenMetadata(groupId.toPath(), artifactId)
    }

    suspend fun fetchArtifact(
        groupId: String,
        artifactId: String,
        version: String
    ): Artifact {
        val path = groupId.toPath()
        return try {
            Artifact(
                type = ArtifactType.JAR,
                inputStream = mavenApi.jar(
                    groupPath = path,
                    artifactId = artifactId,
                    version = version
                ).byteStream()
            )
        } catch (io: HttpException) {
            if (io.code() == 404) {
                Artifact(
                    type = ArtifactType.AAR,
                    inputStream = mavenApi.aar(
                        groupPath = path,
                        artifactId = artifactId,
                        version = version
                    ).byteStream()
                )
            } else {
                throw io
            }
        }
    }

    private fun String.toPath() = replace('.', '/')
}
