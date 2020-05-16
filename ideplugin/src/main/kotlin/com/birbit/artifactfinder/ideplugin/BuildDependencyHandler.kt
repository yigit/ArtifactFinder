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

package com.birbit.artifactfinder.ideplugin

import com.android.ide.common.repository.GradleCoordinate
import com.android.tools.idea.gradle.dependencies.ConfigurationNameMapper
import com.android.tools.idea.gradle.dependencies.GradleDependencyManager
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.openapi.module.Module

class BuildDependencyHandler(private val module: Module) {
    fun addMavenDependency(
        coordinate: String,
        onSuccess: () -> Unit,
        onError: (msg: String) -> Unit
    ) {
        val parsedCoordinate = GradleCoordinate.parseCoordinateString(coordinate)
        val parsedLatestCoordinate = GradleCoordinate.parseCoordinateString(
            parsedCoordinate.groupId + ":" + parsedCoordinate.artifactId + ":+"
        )
        val gradle = GradleDependencyManager.getInstance(module.project)
        val existing = module.getModuleSystem().getResolvedDependency(parsedLatestCoordinate)
        val errorMsg: String? = if (existing == null) {
            val nameMapper = ConfigurationNameMapper { module, name, coordinate ->
                "testImplementation"
            }

            if (gradle.addDependenciesWithoutSync(module, listOf(parsedCoordinate), nameMapper)) {
                null
            } else {
                GENERIC_ERROR
            }
        } else {
            val cmp = existing.version.compareTo(parsedCoordinate.version)
            if (cmp < 0) {
                val updated = gradle.updateLibrariesToVersion(
                    module,
                    listOf(parsedCoordinate),
                    null
                )
                if (updated) {
                    null
                } else {
                    GENERIC_ERROR
                }
            } else {
                "A newer version (${existing.version}) already exists"
            }
        }
        if (errorMsg == null) {
            onSuccess()
        } else {
            onError(errorMsg)
        }
    }

    companion object {
        private const val GENERIC_ERROR = "Gradle Build model cannot be found or is not ready for modifications"
    }
}
