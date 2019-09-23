package com.birbit.artifactfinder.parser.testapk

import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo
import com.birbit.artifactfinder.parser.vo.ParsedClassInfo
import com.birbit.artifactfinder.parser.vo.ParsedExtensionMethodInfo
import com.birbit.artifactfinder.parser.vo.ParsedGlobalMethodInfo
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
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

    fun hasExactGlobalMethods(expected: Collection<ParsedGlobalMethodInfo>): ArtifactInfoSubject {
        check("methods()").that(actual.globalMethods).isEqualTo(expected.toSet())
        return this
    }

    fun hasExactExtensionMethods(expected: Collection<ParsedExtensionMethodInfo>): ArtifactInfoSubject {
        check("extensionMethods()").that(actual.extensionMethods).isEqualTo(expected.toSet())
        return this
    }

    companion object {
        @JvmStatic
        fun artifactInfo(): Subject.Factory<ArtifactInfoSubject, ParsedArtifactInfo> {
            return object : Subject.Factory<ArtifactInfoSubject, ParsedArtifactInfo> {
                override fun createSubject(
                    metadata: FailureMetadata?,
                    actual: ParsedArtifactInfo
                ): ArtifactInfoSubject {
                    return ArtifactInfoSubject(metadata, actual)
                }

            }
        }

        fun assertThat(artifactInfo: ParsedArtifactInfo): ArtifactInfoSubject {
            return Truth.assertAbout(artifactInfo())
                .that(artifactInfo)
        }
    }
}