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

package com.birbit.artifactfinder.parser.testapk

import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo
import com.birbit.artifactfinder.parser.vo.ParsedClassInfo
import com.birbit.artifactfinder.parser.vo.ParsedMethodInfo
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

class ArtifactInfoSubject(
    metadata: FailureMetadata?,
    private val actual: ParsedArtifactInfo
) : Subject(metadata, actual) {
    fun hasExactClasses(expected: Collection<ParsedClassInfo>): ArtifactInfoSubject {
        check("classes()").that(actual.classes).isEqualTo(expected.toSet())
        return this
    }

    fun hasClasses(expected: Collection<ParsedClassInfo>): ArtifactInfoSubject {
        check("some classes()").that(actual.classes).containsAtLeastElementsIn(expected)
        return this
    }

    fun hasExactGlobalMethods(expected: Collection<ParsedMethodInfo>): ArtifactInfoSubject {
        check("methods()").that(actual.methods.filter {
            it.receiver == null
        }.toSet()).isEqualTo(expected.toSet())
        return this
    }

    fun hasExactExtensionMethods(expected: Collection<ParsedMethodInfo>): ArtifactInfoSubject {
        check("extensionMethods()").that(actual.methods.filter {
            it.receiver != null
        }.toSet()).isEqualTo(expected.toSet())
        return this
    }

    companion object {
        @JvmStatic
        fun artifactInfo(): Subject.Factory<ArtifactInfoSubject, ParsedArtifactInfo> {
            return Factory { metadata, actual -> ArtifactInfoSubject(metadata, actual) }
        }

        fun assertThat(artifactInfo: ParsedArtifactInfo): ArtifactInfoSubject {
            return Truth.assertAbout(artifactInfo())
                .that(artifactInfo)
        }
    }
}
