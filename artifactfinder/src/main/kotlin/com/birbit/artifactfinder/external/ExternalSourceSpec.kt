/*
 * Copyright 2020 Google, Inc.
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

package com.birbit.artifactfinder.external

import com.birbit.artifactfinder.ArtifactSource
import com.birbit.artifactfinder.vo.Artifactory
import java.io.InputStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class ExternalSourceSpec(
    // include a spec version so that we can change it w/o making the crawler throw errors
    @SerialName("version")
    val version: Int = LATEST_VERSION,
    // list of packages to crawl
    @SerialName("groups")
    val groups: List<ExternalGroupSpec> = emptyList()
) {
    fun asArtifactSources() = groups.flatMap { pkg ->
        pkg.artifactIds.map { artifactId ->
            ArtifactSource(
                groupId = pkg.groupId,
                artifactId = artifactId,
                artifactory = pkg.artifactory
            )
        }
    }
    companion object {
        val LATEST_VERSION = 1
        private val json = Json(
            JsonConfiguration.Stable.copy(
                strictMode = false,
                prettyPrint = true
            )
        )

        fun parse(input: String): ExternalSourceSpec {
            return json.parse(serializer(), input)
        }

        fun parse(input: InputStream): ExternalSourceSpec {
            return parse(input.reader(Charsets.UTF_8).readText())
        }

        fun print(externalSourceSpec: ExternalSourceSpec): String {
            return json.stringify(serializer(), externalSourceSpec)
        }
    }
}

/**
 * Represents an external source.
 */
@Serializable
data class ExternalGroupSpec(
    @SerialName("groupId")
    val groupId: String,
    @SerialName("artifactIds")
    val artifactIds: List<String> = emptyList(),
    @SerialName("artifactory")
    val artifactory: Artifactory = Artifactory.MAVEN
) {
    init {
        check(artifactIds.isNotEmpty()) {
            "Artifacts for a package cannot be empty"
        }
    }
}
