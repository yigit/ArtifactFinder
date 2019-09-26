package com.birbit.artifactfinder

import com.birbit.artifactfinder.VersionSelector.selectVersions
import com.birbit.artifactfinder.model.Version
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VersionSelectorTest {

    @Test
    fun empty() {
        assertThat(selectVersions(emptyList())).isEmpty()
    }

    @Test
    fun oneOfAny() {
        listOf(
            V_1_1_0,
            V_1_1_0_alpha01,
            V_1_1_0_beta01,
            V_1_1_0_rc01
        ).forEach {
            assertThat(
                selectVersions(
                    listOf(it)
                )
            ).containsExactly(it)
        }
    }

    @Test
    fun threeStable() {
        assertThat(
            selectVersions(
                listOf(
                    V_1_0_0,
                    V_2_0_0,
                    V_1_1_0,
                    V_1_2_0
                )
            )
        ).containsExactly(
            V_2_0_0,
            V_1_2_0,
            V_1_1_0
        )
    }

    @Test
    fun nonStable_hasStable() {
        assertThat(
            selectVersions(
                listOf(
                    V_1_1_0,
                    V_1_1_0_alpha01,
                    V_1_1_0_alpha02,
                    V_1_1_0_beta01,
                    V_1_1_0_beta02,
                    V_1_1_0_rc01,
                    V_1_1_0_rc02
                )
            )
        ).containsExactly(
            V_1_1_0
        )
    }

    @Test
    fun nonStable_hasBetterStable() {
        assertThat(
            selectVersions(
                listOf(
                    V_1_2_0,
                    V_1_1_0_alpha01,
                    V_1_1_0_alpha02,
                    V_1_1_0_beta01,
                    V_1_1_0_beta02,
                    V_1_1_0_rc01,
                    V_1_1_0_rc02
                )
            )
        ).containsExactly(
            V_1_2_0
        )
    }

    @Test
    fun nonStable_hasWorseStable_rc() {
        assertThat(
            selectVersions(
                listOf(
                    V_1_0_0,
                    V_1_1_0_alpha01,
                    V_1_1_0_alpha02,
                    V_1_1_0_beta01,
                    V_1_1_0_beta02,
                    V_1_1_0_rc01,
                    V_1_1_0_rc02
                )
            )
        ).containsExactly(
            V_1_0_0,
            V_1_1_0_rc02
        )
    }

    @Test
    fun nonStable_hasWorseStable_beta() {
        assertThat(
            selectVersions(
                listOf(
                    V_1_0_0,
                    V_1_1_0_alpha01,
                    V_1_1_0_alpha02,
                    V_1_1_0_beta01,
                    V_1_1_0_beta02
                )
            )
        ).containsExactly(
            V_1_0_0,
            V_1_1_0_beta02
        )
    }

    @Test
    fun nonStable_hasWorseStable_alpha() {
        assertThat(
            selectVersions(
                listOf(
                    V_1_0_0,
                    V_1_1_0_alpha01,
                    V_1_1_0_alpha02
                )
            )
        ).containsExactly(
            V_1_0_0,
            V_1_1_0_alpha02
        )
    }


    companion object {
        val V_1_0_0 = Version.fromString("1.0.0")!!
        val V_1_1_0 = Version.fromString("1.1.0")!!
        val V_1_2_0 = Version.fromString("1.2.0")!!
        val V_2_0_0 = Version.fromString("2.0.0")!!
        val V_1_1_0_alpha01 = Version.fromString("1.1.0-alpha01")!!
        val V_1_1_0_alpha02 = Version.fromString("1.1.0-alpha02")!!
        val V_1_1_0_beta01 = Version.fromString("1.1.0-beta01")!!
        val V_1_1_0_beta02 = Version.fromString("1.1.0-beta02")!!
        val V_1_1_0_rc01 = Version.fromString("1.1.0-rc01")!!
        val V_1_1_0_rc02 = Version.fromString("1.1.0-rc02")!!

    }
}