/*
 * Copyright 2020 Google, Inc.
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

package com.birbit.artifactfinder.ideplugin.ui

import com.birbit.artifactfinder.ideplugin.BuildDependencyHandler
import com.birbit.artifactfinder.ideplugin.SearchResultModel
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UI
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import javax.swing.JButton

class SelectDependencyDetailsWindow(
    private val project: Project,
    private val module: Module?,
    private val searchResult: SearchResultModel.SearchResult,
    private val onComplete: () -> Unit
) {
    fun buildAndShow(point: RelativePoint) {
        val allModuleChoices = project.allModules().toTypedArray()
        val moduleCombo = ComboBox<Module>(allModuleChoices).also {
            if (module != null) {
                it.selectedItem = module
            }
        }
        var popup: JBPopup? = null
        val versionCombo = ComboBox(searchResult.versions.toTypedArray())
        val button = JButton("Add", AllIcons.General.Add).apply {
            isOpaque = true
            isEnabled = true
            addActionListener {
                val module = moduleCombo.selectedItem as? Module ?: return@addActionListener
                val version = versionCombo.selectedItem as? String ?: return@addActionListener
                addToGradle(
                    version = version,
                    module = module,
                    includeProcessor = false
                )
                popup?.cancel()
            }
        }
        val root = UI.PanelFactory.grid()
            .add(
                UI.PanelFactory.panel(moduleCombo)
            ).add(
                UI.PanelFactory.panel(versionCombo)
            ).add(
                UI.PanelFactory.panel(button)
            )
            .createPanel()

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(root, versionCombo)
            .also {
                it.setFocusable(true)
                it.setRequestFocus(true)
                it.setResizable(true)
                it.setTitle("Select Version & Scope")
                it.setCancelOnClickOutside(true)
                it.setMovable(true)
                it.addListener(object : JBPopupListener {
                    override fun onClosed(event: LightweightWindowEvent) {
                        super.onClosed(event)
                        onComplete()
                    }
                })
                it.setShowBorder(true)
                    .createPopup().apply {
                        popup = this
                        show(point)
                    }
            }
    }

    private fun addToGradle(version: String, module: Module, includeProcessor: Boolean) {
        val dependencyUtil = BuildDependencyHandler(module)
        val artifact = searchResult.qualifiedArtifact(version)
        dependencyUtil.addMavenDependency(
            coordinate = artifact,
            onSuccess = {
                showNotification("Added $artifact to ${module.name}'s dependencies.")
            },
            onError = { msg ->
                showError("Unable to add $artifact to ${module.name}'s dependencies. $msg")
            }
        )
    }

    private fun showNotification(msg: String) {
        Notifications.Bus.notify(
            Notification("artifact-finder", "Artifact Finder", msg, NotificationType.INFORMATION),
            project
        )
    }

    private fun showError(msg: String) {
        Notifications.Bus.notify(
            Notification("artifact-finder", "Artifact Finder", msg, NotificationType.ERROR),
            project
        )
    }
}