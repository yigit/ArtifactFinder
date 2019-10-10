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
