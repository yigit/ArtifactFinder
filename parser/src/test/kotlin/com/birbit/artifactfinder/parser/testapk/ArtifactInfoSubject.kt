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