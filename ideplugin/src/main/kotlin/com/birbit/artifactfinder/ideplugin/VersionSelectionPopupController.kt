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

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

@Suppress("UNUSED_PARAMETER")
class VersionSelectionPopupController(
    private val result: SearchResultModel.SearchResult,
    private val project: Project,
    private val currentModule: Module?,
    private val callback: Callback
) {

    fun buildPopup(): ListPopup {
        val popupStep = VersionStep(
            versions = result.versions
        ) {
            handleChoice(it)
            callback.onChosen()
        }
        return JBPopupFactory.getInstance()
            .createListPopup(popupStep)
    }

    private fun handleChoice(choice: Choice) {
        checkNotNull(choice.version) {
            "must choose a version"
        }
        checkNotNull(choice.action) {
            "must choose an action"
        }
        check(!choice.action.needsModule || choice.module != null) {
            "should either pick an action that does not require a module or pick a module"
        }
        when (choice.action) {
            Action.ADD_TO_GRADLE -> {
                addToGradle(choice.version, choice.module!!, false)
            }
            Action.ADD_TO_GRADLE_WITH_PROCESSOR -> {
                addToGradle(choice.version, choice.module!!, true)
            }
            Action.COPY_COORDINATES -> {
                copyToClipboard(choice.version, false)
            }
            Action.COPY_WITH_PROCESSOR -> {
                copyToClipboard(choice.version, true)
            }
        }
    }

    private fun addToGradle(version: String, module: Module, includeProcessor: Boolean) {
        val dependencyUtil = BuildDependencyHandler(module)
        val artifact = result.qualifiedArtifact(version)
        dependencyUtil.addMavenDependency(artifact)
        showNotification("Added $artifact to ${module.name}'s dependencies")
    }

    private fun copyToClipboard(version: String, includeProcessor: Boolean) {
        val artifact = result.qualifiedArtifact(version)
        val selection = StringSelection(artifact)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        showNotification("Copied $artifact into clipboard")
    }

    private fun showNotification(msg: String) {
        Notifications.Bus.notify(
            Notification("artifact-finder", "Artifact Finder", msg, NotificationType.INFORMATION),
            project
        )
    }

    interface Callback {
        fun onChosen()
        fun onCancel()
    }

    private inner class VersionStep(
        versions: List<String>,
        private val callback: (Choice) -> Unit
    ) : BaseListPopupStep<String>("versions", versions) {
        override fun hasSubstep(selectedValue: String?): Boolean {
            return true
        }

        override fun getTextFor(value: String?): String {
            return value ?: "?"
        }

        override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
            if (selectedValue != null) {
                return ActionStep(
                    choice = Choice(
                        version = selectedValue
                    ),
                    callback = callback
                )
            }
            return PopupStep.FINAL_CHOICE
        }

        override fun isSpeedSearchEnabled() = true
    }

    private inner class ActionStep(
        private val choice: Choice,
        private val callback: (Choice) -> Unit
    ) : BaseListPopupStep<Action>("action", Action.getList(false)) {
        override fun hasSubstep(selectedValue: Action?): Boolean {
            return selectedValue?.needsModule == true
        }

        override fun getTextFor(action: Action?): String {
            return action?.text ?: "?"
        }

        override fun onChosen(selectedValue: Action?, finalChoice: Boolean): PopupStep<*>? {
            if (selectedValue != null) {
                val newChoice = choice.copy(
                    action = selectedValue
                )
                if (selectedValue.needsModule) {
                    return ModuleSelectionStep(newChoice, callback)
                } else if (finalChoice) {
                    callback(newChoice)
                }
            }
            return PopupStep.FINAL_CHOICE
        }

        override fun isSpeedSearchEnabled() = true
    }

    private inner class ModuleSelectionStep(
        private val choice: Choice,
        private val callback: (Choice) -> Unit
    ) : BaseListPopupStep<Module>("module", project.allModules()) {
        override fun getDefaultOptionIndex(): Int {
            return project.allModules().indexOf(currentModule)
        }

        override fun hasSubstep(selectedValue: Module?): Boolean {
            return false
        }

        override fun getTextFor(module: Module?): String {
            return module?.name ?: "?"
        }

        override fun onChosen(selectedValue: Module?, finalChoice: Boolean): PopupStep<*>? {
            if (finalChoice && selectedValue != null) {
                callback(
                    choice.copy(
                        module = selectedValue
                    )
                )
            }
            return PopupStep.FINAL_CHOICE
        }

        override fun isSpeedSearchEnabled() = true
    }

    private data class Choice(
        val version: String,
        val action: Action? = null,
        val module: Module? = null
    )

    private enum class Action(
        val text: String,
        val needsModule: Boolean
    ) {
        COPY_COORDINATES("Copy coordinates", false),
        COPY_WITH_PROCESSOR("Copy with annotation processor", false),
        ADD_TO_GRADLE("Add to gradle", true),
        ADD_TO_GRADLE_WITH_PROCESSOR("Add to gradle with processor", true);

        companion object {
            fun getList(hasProcessor: Boolean): List<Action> {
                return if (hasProcessor) {
                    values().toList()
                } else {
                    listOf(COPY_COORDINATES, ADD_TO_GRADLE)
                }
            }
        }
    }
}
