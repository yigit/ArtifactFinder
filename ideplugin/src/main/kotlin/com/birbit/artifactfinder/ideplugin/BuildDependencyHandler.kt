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
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.module.Module

class BuildDependencyHandler(private val module: Module) {
    fun addMavenDependency(coordinate: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val parsedCoordinate = GradleCoordinate.parseCoordinateString(coordinate)

        module.getModuleSystem().registerDependency(parsedCoordinate)

        // There is no callback from Gradle about the fact that a dependency was registered, so we should check that
        val resolvedDependency = module.getModuleSystem().getResolvedDependency(parsedCoordinate)
        if (resolvedDependency != null) {
            sync()
            onSuccess()
        } else {
            onError.invoke()
        }
    }

    private fun sync() {
        DataManager.getInstance().dataContextFromFocusAsync.onSuccess {
            val am = ActionManager.getInstance()
            val syncAction = am.getAction("Android.SyncProject")
            val fakeActionEvent = AnActionEvent(
                null, it,
                ActionPlaces.UNKNOWN, Presentation(),
                ActionManager.getInstance(), 0
            )
            syncAction.actionPerformed(fakeActionEvent)
        }
    }
}
