package com.birbit.artifactfinder

import com.birbit.artifactfinder.vo.Artifactory.MAVEN

// https://repo.maven.apache.org/maven2/ does not provide maven-metadata anymore :/
private val RETROFIT = listOf(
    "adapter-guava",
    "adapter-java8",
    "adapter-rxjava",
    "adapter-rxjava2",
    "adapter-scala",
    "converter-gson",
    "converter-guava",
    "converter-jackson",
    "converter-java8",
    "converter-jaxb",
    "converter-moshi",
    "converter-protobuf",
    "converter-scalars",
    "converter-simplexml",
    "converter-wire",
    "retrofit",
    "retrofit-adapters",
    "retrofit-converters",
    "retrofit-mock"
).map {
    ArtifactSource(
        groupId = "com.squareup.retrofit2",
        artifactId = it,
        artifactory = MAVEN
    )
}
// TODO move to an external file that can be fetched
val EXTERNAL_SOURCES = FetcherSource(
    groups = emptyList(),
    artifacts = listOf(
        ArtifactSource(
            groupId = "com.github.bumptech.glide",
            artifactId = "glide",
            artifactory = MAVEN
        ),
        ArtifactSource(
            groupId = "com.google.dagger",
            artifactId = "dagger",
            artifactory = MAVEN
        )
    ) + RETROFIT

)