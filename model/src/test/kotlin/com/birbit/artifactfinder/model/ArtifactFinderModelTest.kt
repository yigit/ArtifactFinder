package com.birbit.artifactfinder.model

import com.birbit.artifactfinder.parser.vo.ParsedArtifactInfo
import com.birbit.artifactfinder.parser.vo.ParsedClassInfo
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors

@RunWith(JUnit4::class)
class ArtifactFinderModelTest {

    private val executor = Executors.newSingleThreadExecutor()
    private val db = ArtifactFinderDatabase.create(
        name = null,
        readExecutor = executor,
        writeExecutor = executor
    )
    private val model = ArtifactFinderModel(
        db
    )

    @Test
    fun simpleIndexRead() = runBlocking<Unit> {
        val addedPending = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION)
        Truth.assertThat(addedPending).isTrue()
        model.saveParsedArtifact(
            pendingArtifact = PendingArtifact(
                id = 1,
                groupId = ARTIFACT_GROUP_ID,
                artifactId = ARTIFACT_ID,
                version = ARTIFACT_VERSION
            ),
            info = INFO
        )

        Truth.assertThat(model.search(CLASS_NAME))
            .containsExactly(EXPECTED)
        Truth.assertThat(model.search(CLASS_NAME.toLowerCase()))
            .containsExactly(EXPECTED)
        Truth.assertThat(model.search(CLASS_NAME.toUpperCase()))
            .containsExactly(EXPECTED)
        Truth.assertThat(model.search("NotRight"))
            .isEmpty()

        val addedPending3 = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID_3,
            artifactId = ARTIFACT_ID_3,
            version = ARTIFACT_VERSION)
        Truth.assertThat(addedPending3).isTrue()
        model.saveParsedArtifact(
            pendingArtifact = PendingArtifact(
                id = 2,
                groupId = ARTIFACT_GROUP_ID_3,
                artifactId = ARTIFACT_ID_3,
                version = ARTIFACT_VERSION
            ),
            info = INFO_3
        )
        Truth.assertThat(model.search(CLASS_NAME_3))
            .containsExactly(EXPECTED_3)

    }

    @Test
    fun nestedClassReadWrite() = runBlocking<Unit> {
        val addedPending = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION)
        Truth.assertThat(addedPending).isTrue()
        model.saveParsedArtifact(
            pendingArtifact = PendingArtifact(
                id = 1,
                groupId = ARTIFACT_GROUP_ID,
                artifactId = ARTIFACT_ID,
                version = ARTIFACT_VERSION
            ),
            info = INFO_2
        )

        Truth.assertThat(model.search(CLASS_NAME_2))
            .containsExactly(EXPECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2.toLowerCase()))
            .containsExactly(EXPECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2.toUpperCase()))
            .containsExactly(EXPECTED_2)

        Truth.assertThat(model.search(CLASS_NAME_2_INNER))
            .containsExactly(EXPECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2_INNER.toLowerCase()))
            .containsExactly(EXPECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2_INNER.toUpperCase()))
            .containsExactly(EXPECTED_2)

        Truth.assertThat(model.search(CLASS_NAME_2_OUTER))
            .containsExactly(EXPECTED_2)

        Truth.assertThat(model.search(CLASS_NAME_2.replace('$', '.')))
            .containsExactly(EXPECTED_2)
        Truth.assertThat(model.search("NotRight"))
            .isEmpty()
        Truth.assertThat(model.search("NotRight"))
            .isEmpty()
    }

    @Test
    fun addGetPending() = runBlocking<Unit> {
        Truth.assertThat(model.findNextPendingArtifact(emptyList())).isNull()
        val addResult = model.addPendingArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION
        )
        Truth.assertThat(addResult).isTrue()
        val pending = PendingArtifact(
            id = 1,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            retries = 0,
            fetched = false
        )
        Truth.assertThat(model.findNextPendingArtifact(emptyList()))
            .isEqualTo(pending)
        Truth.assertThat(model.findNextPendingArtifact(listOf(pending.id)))
            .isNull()
        model.incrementPendingArtifactRetry(pending)
        Truth.assertThat(model.findNextPendingArtifact(emptyList()))
            .isEqualTo(pending.copy(retries = 1))
        model.saveParsedArtifact(
            pendingArtifact = pending,
            info = ParsedArtifactInfo()
        )
        Truth.assertThat(model.findNextPendingArtifact(emptyList())).isNull()
    }

    companion object {
        private val ARTIFACT_GROUP_ID = "foo.bar"
        private val ARTIFACT_ID = "baz"
        private val ARTIFACT_GROUP_ID_3 = "foo.bak"
        private val ARTIFACT_ID_3 = "ban"
        private val ARTIFACT_VERSION = Version(1, 1, 0, extra = null)
        private val CLASS_PKG = "foo.bar.pkg1"
        private val CLASS_NAME = "Bar"

        private val CLASS_PKG_2 = "foo.bar.pkg2"
        private val CLASS_NAME_2_OUTER = "Foo"
        private val CLASS_NAME_2_INNER = "Baz"
        private val CLASS_NAME_2 = "${CLASS_NAME_2_OUTER}\$${CLASS_NAME_2_INNER}"

        private val CLASS_PKG_3 = "foo.bar.pkg3"
        private val CLASS_NAME_3 = "Bap"


        private val EXPECTED = SearchRecord(
            pkg = CLASS_PKG,
            name = CLASS_NAME,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION
        )
        private val INFO = ParsedArtifactInfo(
            classes = setOf(
                ParsedClassInfo(
                    pkg = CLASS_PKG,
                    name = CLASS_NAME
                )
            ),
            globalMethods = emptySet(),
            extensionMethods = emptySet()
        )

        private val INFO_2 = ParsedArtifactInfo(
            classes = setOf(
                ParsedClassInfo(
                    pkg = CLASS_PKG_2,
                    name = CLASS_NAME_2
                )
            ),
            globalMethods = emptySet(),
            extensionMethods = emptySet()
        )

        private val EXPECTED_2 = SearchRecord(
            pkg = CLASS_PKG_2,
            name = CLASS_NAME_2,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION
        )

        private val INFO_3 = ParsedArtifactInfo(
            classes = setOf(
                ParsedClassInfo(
                    pkg = CLASS_PKG_3,
                    name = CLASS_NAME_3
                )
            ),
            globalMethods = emptySet(),
            extensionMethods = emptySet()
        )

        private val EXPECTED_3 = SearchRecord(
            pkg = CLASS_PKG_3,
            name = CLASS_NAME_3,
            groupId = ARTIFACT_GROUP_ID_3,
            artifactId = ARTIFACT_ID_3,
            version = ARTIFACT_VERSION
        )
    }
}