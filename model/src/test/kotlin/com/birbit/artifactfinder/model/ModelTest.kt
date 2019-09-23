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
class ModelTest {

    private val executor = Executors.newSingleThreadExecutor()
    private val db = ArtifactFinderDatabase.create(
        name = null,
        executor = executor
    )
    val model = ArtifactFinderModel(
        db
    )

    @Test
    fun simpleIndexRead() = runBlocking<Unit> {
        model.saveParsedArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            info = INFO
        )

        Truth.assertThat(model.search(CLASS_NAME))
            .containsExactly(EXOECTED)
        Truth.assertThat(model.search(CLASS_NAME.toLowerCase()))
            .containsExactly(EXOECTED)
        Truth.assertThat(model.search(CLASS_NAME.toUpperCase()))
            .containsExactly(EXOECTED)
        Truth.assertThat(model.search("NotRight"))
            .isEmpty()
    }

    @Test
    fun nestedClassReadWrite() = runBlocking<Unit> {
        model.saveParsedArtifact(
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION,
            info = INFO_2
        )

        Truth.assertThat(model.search(CLASS_NAME_2))
            .containsExactly(EXOECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2.toLowerCase()))
            .containsExactly(EXOECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2.toUpperCase()))
            .containsExactly(EXOECTED_2)

        Truth.assertThat(model.search(CLASS_NAME_2_INNER))
            .containsExactly(EXOECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2_INNER.toLowerCase()))
            .containsExactly(EXOECTED_2)
        Truth.assertThat(model.search(CLASS_NAME_2_INNER.toUpperCase()))
            .containsExactly(EXOECTED_2)

        Truth.assertThat(model.search(CLASS_NAME_2_OUTER))
            .containsExactly(EXOECTED_2)

        Truth.assertThat(model.search(CLASS_NAME_2.replace('$', '.')))
            .containsExactly(EXOECTED_2)
        Truth.assertThat(model.search("NotRight"))
            .isEmpty()
        Truth.assertThat(model.search("NotRight"))
            .isEmpty()
    }

    companion object {
        private val ARTIFACT_GROUP_ID = "foo.bar"
        private val ARTIFACT_ID = "baz"
        private val ARTIFACT_VERSION = Version(1, 1, 0, extra = null)
        private val CLASS_PKG = "foo.bar.pkg1"
        private val CLASS_NAME = "Bar"

        private val CLASS_PKG_2 = "foo.bar.pkg2"
        private val CLASS_NAME_2_OUTER = "Foo"
        private val CLASS_NAME_2_INNER = "Baz"
        private val CLASS_NAME_2 = "${CLASS_NAME_2_OUTER}\$${CLASS_NAME_2_INNER}"
        private val EXOECTED = SearchRecord(
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
        private val EXOECTED_2 = SearchRecord(
            pkg = CLASS_PKG_2,
            name = CLASS_NAME_2,
            groupId = ARTIFACT_GROUP_ID,
            artifactId = ARTIFACT_ID,
            version = ARTIFACT_VERSION
        )

    }
}