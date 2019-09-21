package com.birbit.artifactfinder.parser.testapk

import com.birbit.artifactfinder.parser.vo.ArtifactInfo
import com.birbit.artifactfinder.parser.vo.ClassInfo
import com.birbit.artifactfinder.parser.vo.ExtensionMethodInfo
import com.birbit.artifactfinder.parser.vo.GlobalMethodInfo
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth

class ArtifactInfoSubject(
    metadata: FailureMetadata?,
    private val actual: ArtifactInfo
) : Subject(metadata, actual) {
    fun hasExactClasses(expected: Collection<ClassInfo>): ArtifactInfoSubject {
        check("classes()").that(actual.classes).isEqualTo(expected.toSet())
        return this
    }

    fun hasClasses(expected: Collection<ClassInfo>): ArtifactInfoSubject {
        check("some classes()").that(actual.classes).containsAtLeastElementsIn(expected)
        return this
    }

    fun hasExactGlobalMethods(expected: Collection<GlobalMethodInfo>): ArtifactInfoSubject {
        check("methods()").that(actual.globalMethods).isEqualTo(expected.toSet())
        return this
    }

    fun hasExactExtensionMethods(expected: Collection<ExtensionMethodInfo>): ArtifactInfoSubject {
        check("extensionMethods()").that(actual.extensionMethods).isEqualTo(expected.toSet())
        return this
    }

    companion object {
        @JvmStatic
        fun artifactInfo(): Subject.Factory<ArtifactInfoSubject, ArtifactInfo> {
            return object : Subject.Factory<ArtifactInfoSubject, ArtifactInfo> {
                override fun createSubject(
                    metadata: FailureMetadata?,
                    actual: ArtifactInfo
                ): ArtifactInfoSubject {
                    return ArtifactInfoSubject(metadata, actual)
                }

            }
        }

        fun assertThat(artifactInfo: ArtifactInfo): ArtifactInfoSubject {
            return Truth.assertAbout(artifactInfo())
                .that(artifactInfo)
        }
    }
}