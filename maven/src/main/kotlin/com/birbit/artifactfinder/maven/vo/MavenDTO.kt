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
