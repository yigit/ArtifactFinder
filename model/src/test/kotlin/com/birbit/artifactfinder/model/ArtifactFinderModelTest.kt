package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.model.SearchRecord.Type.*
import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo
import com.birbit.artifactfinder.parser.vo.ParsedClassInfo
import com.birbit.artifactfinder.parser.vo.ParsedMethodInfo
import com.birbit.artifactfinder.vo.Artifactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class ArtifactFinderModelTest {
    private val scope = TestCoroutineScope()
    private val model = ArtifactFinderModel(null)

    private suspend fun ArtifactFinderModel.search(query:String) = search(
        ArtifactFinderModel.SearchParams(
            query = query,
            includeGlobalMethods = false,
            includeExtensionMethods = false,
            includeClasses = true
        )
    )
    @Test
    fun simpleIndexRead() = scope.runBlockingTest {
        val addedPending = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            artifactory = Artifactory.MAVEN
        )
        assertThat(addedPending).isTrue()
        model.saveParsedArtifact(
            pendingArtifact = PendingArtifact(
                id = 1,
                groupId = ARTIFACT_GROUP_ID,
                artifactId = ARTIFACT_ID,
                version = ARTIFACT_VERSION,
                artifactory = Artifactory.MAVEN
            ),
            info = INFO
        )

        assertThat(model.search(CLASS_NAME))
            .containsExactly(EXPECTED)
        assertThat(model.search(CLASS_NAME.toLowerCase()))
            .containsExactly(EXPECTED)
        assertThat(model.search(CLASS_NAME.toUpperCase()))
            .containsExactly(EXPECTED)
        assertThat(model.search("NotRight"))
            .isEmpty()

        val addedPending3 = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID_3,
            artifactId = ARTIFACT_ID_3,
            version = ARTIFACT_VERSION,
            artifactory = Artifactory.MAVEN
        )
        assertThat(addedPending3).isTrue()
        model.saveParsedArtifact(
            pendingArtifact = PendingArtifact(
                id = 2,
                groupId = ARTIFACT_GROUP_ID_3,
                artifactId = ARTIFACT_ID_3,
                version = ARTIFACT_VERSION,
                artifactory = Artifactory.MAVEN
            ),
            info = INFO_3
        )
        assertThat(model.search(CLASS_NAME_3))
            .containsExactly(EXPECTED_3)

    }

    @Test
    fun nestedClassReadWrite() = scope.runBlockingTest {
        val addedPending = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            artifactory = Artifactory.MAVEN
        )
        assertThat(addedPending).isTrue()
        model.saveParsedArtifact(
            pendingArtifact = PendingArtifact(
                id = 1,
                groupId = ARTIFACT_GROUP_ID,
                artifactId = ARTIFACT_ID,
                version = ARTIFACT_VERSION,
                artifactory = Artifactory.MAVEN
            ),
            info = INFO_2
        )

        assertThat(model.search(CLASS_NAME_2))
            .containsExactly(EXPECTED_2)
        assertThat(model.search(CLASS_NAME_2.toLowerCase()))
            .containsExactly(EXPECTED_2)
        assertThat(model.search(CLASS_NAME_2.toUpperCase()))
            .containsExactly(EXPECTED_2)

        assertThat(model.search(CLASS_NAME_2_INNER))
            .containsExactly(EXPECTED_2)
        assertThat(model.search(CLASS_NAME_2_INNER.toLowerCase()))
            .containsExactly(EXPECTED_2)
        assertThat(model.search(CLASS_NAME_2_INNER.toUpperCase()))
            .containsExactly(EXPECTED_2)

        assertThat(model.search(CLASS_NAME_2_OUTER))
            .containsExactly(EXPECTED_2)

        assertThat(model.search(CLASS_NAME_2.replace('$', '.')))
            .containsExactly(EXPECTED_2)
        assertThat(model.search("NotRight"))
            .isEmpty()
        assertThat(model.search("NotRight"))
            .isEmpty()
    }

    @Test
    fun addGetPending() = scope.runBlockingTest {
        assertThat(model.findNextPendingArtifact(emptyList())).isNull()
        val addResult = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            artifactory = Artifactory.MAVEN
        )
        assertThat(addResult).isTrue()
        val pending = PendingArtifact(
            id = 1,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            retries = 0,
            fetched = false,
            artifactory = Artifactory.MAVEN
        )
        assertThat(model.findNextPendingArtifact(emptyList()))
            .isEqualTo(pending)
        assertThat(model.findNextPendingArtifact(listOf(pending.id)))
            .isNull()
        model.incrementPendingArtifactRetry(pending)
        assertThat(model.findNextPendingArtifact(emptyList()))
            .isEqualTo(pending.copy(retries = 1))
        model.saveParsedArtifact(
            pendingArtifact = pending,
            info = ParsedArtifactInfo()
        )
        assertThat(model.findNextPendingArtifact(emptyList())).isNull()
    }

    @Test
    fun methods() = scope.runBlockingTest {
        val addedPending = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            artifactory = Artifactory.MAVEN
        )
        assertThat(addedPending).isTrue()
        model.saveParsedArtifact(
            pendingArtifact = PendingArtifact(
                id = 1,
                groupId = ARTIFACT_GROUP_ID,
                artifactId = ARTIFACT_ID,
                version = ARTIFACT_VERSION,
                artifactory = Artifactory.MAVEN
            ),
            info = INFO_WITH_METHODS
        )

        assertThat(model.search(
            ArtifactFinderModel.SearchParams(
                query = EXTENSION_METHOD_NAME,
                includeClasses = true,
                includeExtensionMethods = true,
                includeGlobalMethods = true
            )
        )).containsExactly(EXPECTED_EXTENSION_METHOD)

        assertThat(model.search(
            ArtifactFinderModel.SearchParams(
                query = GLOBAL_METHOD_NAME,
                includeClasses = false,
                includeExtensionMethods = true,
                includeGlobalMethods = true
            )
        )).containsExactly(EXPECTED_GLOBAL_METHOD)

        assertThat(model.search(
            ArtifactFinderModel.SearchParams(
                query = "meth",
                includeClasses = false,
                includeExtensionMethods = true,
                includeGlobalMethods = true
            )
        )).containsExactly(EXPECTED_GLOBAL_METHOD, EXPECTED_EXTENSION_METHOD)

        assertThat(model.search(
            ArtifactFinderModel.SearchParams(
                query = "meth",
                includeClasses = false,
                includeExtensionMethods = false,
                includeGlobalMethods = true
            )
        )).containsExactly(EXPECTED_GLOBAL_METHOD)

        assertThat(model.search(
            ArtifactFinderModel.SearchParams(
                query = "meth",
                includeClasses = false,
                includeExtensionMethods = true,
                includeGlobalMethods = false
            )
        )).containsExactly(EXPECTED_EXTENSION_METHOD)

        assertThat(model.search(
            ArtifactFinderModel.SearchParams(
                query = "meth",
                includeClasses = false,
                includeExtensionMethods = false,
                includeGlobalMethods = false
            )
        )).isEmpty()
    }

    companion object {
        private const val ARTIFACT_GROUP_ID = "foo.bar"
        private const val ARTIFACT_ID = "baz"
        private const val ARTIFACT_GROUP_ID_3 = "foo.bak"
        private const val ARTIFACT_ID_3 = "ban"
        private val ARTIFACT_VERSION = Version(1, 1, 0, extra = null)
        private const val CLASS_PKG = "foo.bar.pkg1"
        private const val CLASS_NAME = "Bar"

        private const val CLASS_PKG_2 = "foo.bar.pkg2"
        private const val CLASS_NAME_2_OUTER = "Foo"
        private const val CLASS_NAME_2_INNER = "Baz"
        private const val CLASS_NAME_2 = "${CLASS_NAME_2_OUTER}\$${CLASS_NAME_2_INNER}"

        private const val CLASS_PKG_3 = "foo.bar.pkg3"
        private const val CLASS_NAME_3 = "Bap"

        private const val EXTENSION_METHOD_PKG = "m.x"
        private const val EXTENSION_METHOD_NAME = "methodExt"

        private const val GLOBAL_METHOD_PKG = "g.x"
        private const val GLOBAL_METHOD_NAME = "methodGlobal"


        private val EXPECTED = SearchRecord(
            pkg = CLASS_PKG,
            name = CLASS_NAME,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            type = CLASS,
            receiverName = null
        )
        private val INFO = ParsedArtifactInfo(
            classes = setOf(
                ParsedClassInfo(
                    pkg = CLASS_PKG,
                    name = CLASS_NAME
                )
            ),
            methods = emptySet()
        )

        private val INFO_2 = ParsedArtifactInfo(
            classes = setOf(
                ParsedClassInfo(
                    pkg = CLASS_PKG_2,
                    name = CLASS_NAME_2
                )
            ),
            methods = emptySet()
        )

        private val EXPECTED_2 = SearchRecord(
            pkg = CLASS_PKG_2,
            name = CLASS_NAME_2,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            type = CLASS,
            receiverName = null
        )

        private val INFO_3 = ParsedArtifactInfo(
            classes = setOf(
                ParsedClassInfo(
                    pkg = CLASS_PKG_3,
                    name = CLASS_NAME_3
                )
            ),
            methods = emptySet()
        )

        private val EXPECTED_3 = SearchRecord(
            pkg = CLASS_PKG_3,
            name = CLASS_NAME_3,
            groupId = ARTIFACT_GROUP_ID_3,
            artifactId = ARTIFACT_ID_3,
            version = ARTIFACT_VERSION,
            type = CLASS,
            receiverName = null
        )

        private val INFO_WITH_METHODS = ParsedArtifactInfo(
            classes = emptySet(),
            methods = setOf(
                ParsedMethodInfo(
                    pkg = EXTENSION_METHOD_PKG,
                    name = EXTENSION_METHOD_NAME,
                    receiver = ParsedClassInfo(
                        pkg = CLASS_PKG,
                        name = CLASS_NAME
                    )
                ),
                ParsedMethodInfo(
                    pkg = GLOBAL_METHOD_PKG,
                    name = GLOBAL_METHOD_NAME,
                    receiver = null
                )
            )
        )

        private val EXPECTED_EXTENSION_METHOD = SearchRecord(
            pkg = EXTENSION_METHOD_PKG,
            name = EXTENSION_METHOD_NAME,
            type = EXTENSION_METHOD,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            receiverName = CLASS_NAME
        )

        private val EXPECTED_GLOBAL_METHOD = SearchRecord(
            pkg = GLOBAL_METHOD_PKG,
            name = GLOBAL_METHOD_NAME,
            type = GLOBAL_METHOD,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            receiverName = null
        )
    }
}