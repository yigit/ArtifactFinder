package com.birbit.artifactfinder.maven.vo

import java.io.InputStream

// from: https://cwiki.apache.org/confluence/display/MAVEN/Remote+repository+layout
enum class Artifactory(val baseUrl: String) {
    GOOGLE("https://dl.google.com/dl/android/maven2/"),
    MAVEN("https://repo.maven.apache.org/maven2/")
}

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
