package com.birbit.artifactfinder.maven.vo

import java.io.InputStream

data class GroupIndex(val groupId: String, val artifactIds: Set<String>)

data class ArtifactMetadata(val groupId: String, val artifactId: String, val versioning: Versioning)

data class Versioning(val latest: String, val release: String, val versions: Set<String>)

enum class ArtifactType {
    JAR,
    AAR
}

data class Artifact(
    val type: ArtifactType,
    val inputStream: InputStream
)
