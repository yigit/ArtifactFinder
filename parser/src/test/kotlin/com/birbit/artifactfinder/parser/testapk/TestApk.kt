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

import com.birbit.artifactfinder.parser.Aar
import com.birbit.artifactfinder.parser.testapk.templates.androidManifest
import com.birbit.artifactfinder.parser.testapk.templates.gradlePropsFile
import com.birbit.artifactfinder.parser.testapk.templates.mainBuildFile
import com.birbit.artifactfinder.parser.testapk.templates.moduleBuildFile
import com.birbit.artifactfinder.parser.testapk.templates.settingsGradle
import com.google.common.truth.Truth
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class SourceFile(
    val path: String,
    val code: String
)

class TestApk(
    private val tmpFolder: File,
    private val sources: List<SourceFile>,
    private val gradleVersion: String = "5.6.1",
    private val agpVersion: String = "3.5.0",
    private val kotlinVersion: String = "1.3.50",
    private val appPkg: String = "com.test"
) {
    private fun prepareProject() {
        tmpFolder.deleteRecursively()
        tmpFolder.mkdirs()
        tmpFolder.resolve("gradle/wrapper").mkdirs()
        tmpFolder.resolve("gradle/wrapper/gradle-wrapper.properties")
            .writeText(gradlePropsFile(gradleVersion))
        tmpFolder.resolve("gradle/wrapper/gradle-wrapper.jar")
            .writeBytes(
                TestApk::class.java.getResourceAsStream("/gradle-wrapper.jar")
                    .readBytes()
            )
        tmpFolder.resolve("gradlew").apply {
            writeBytes(
                TestApk::class.java.getResourceAsStream("/gradlew")
                    .readBytes()
            )
            setExecutable(true)
        }
        tmpFolder.resolve("build.gradle").writeText(
            mainBuildFile(
                agpVersion = agpVersion,
                kotlinVersion = kotlinVersion
            )
        )
        tmpFolder.resolve("settings.gradle").writeText(
            settingsGradle()
        )
        tmpFolder.resolve("lib").mkdirs()
        tmpFolder.resolve("lib").resolve("build.gradle")
            .writeText(
                moduleBuildFile(
                    hasKotlin = true
                )
            )
        val srcRoot = tmpFolder.resolve("lib/src/main/java")
        srcRoot.mkdirs()
        sources.forEach {
            srcRoot.resolve(it.path).apply {
                parentFile.mkdirs()
                writeText(it.code)
            }
        }

        tmpFolder.resolve("lib/src/main/AndroidManifest.xml")
            .writeText(androidManifest(appPkg))
    }

    fun buildAar(): Aar {
        prepareProject()
        @Suppress("UnstableApiUsage")
        val result = GradleRunner.create()
            .withProjectDir(tmpFolder)
            .withArguments(":lib:assembleRelease")
            .withEnvironment(
                mapOf(
                    "ANDROID_HOME" to "/home/yboyar/android/sdk" // TODO
                )
            )
            .build()
        val output = tmpFolder.resolve("lib/build/outputs/aar/lib-release.aar")
        Truth.assertThat(output.exists()).isTrue()
        val assembleTask = result.tasks.first {
            it.path == ":lib:assembleRelease"
        }
        Truth.assertThat(assembleTask.outcome).isEqualTo(
            TaskOutcome.SUCCESS
        )
        return Aar(output.inputStream())
    }
}
