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

package com.birbit.artifactfinder.ideplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

@FlowPreview
@ExperimentalCoroutinesApi
class SearchArtifactPanelController(
    private val project: Project,
    private val module: Module? = null,
    private val initialText: String? = null
) {
    private val job = SupervisorJob(null)
    private val scope = CoroutineScope(Dispatchers.Main + job)

    fun buildAndShow() {
        val searchResultModel = SearchResultTableModel()
        val resultTable = JBTable(searchResultModel).also {
            it.setMaxItemsForSizeCalculation(10)
            it.setShowColumns(true)
            it.autoResizeMode = JBTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            it.fillsViewportHeight = true
//            it.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
//
//            it.selectionModel.addListSelectionListener {
//                if (it.valueIsAdjusting) {
//                    return@addListSelectionListener
//                }
//                val searchResult = searchResultModel.getItem(it.firstIndex) ?: return@addListSelectionListener
//                println(searchResult)
//                SelectDependencyDetailsWindow(
//                    project= project,
//                    module = module,
//                    searchResult = searchResult
//                ).buildAndShow()
//            }
        }

        val inputText = JBTextField(initialText)
        inputText.emptyText.text = "input a class name. e.g: RecyclerView"
        val errorText = JBLabel(
            "", AllIcons.General.Error, JBLabel.LEADING
        )

        resultTable.putClientProperty(UIUtil.KEEP_BORDER_SIDES, SideBorder.ALL)

        val bottomPanel = JPanel()
        bottomPanel.layout = BoxLayout(bottomPanel, BoxLayout.Y_AXIS)
        val helpButton = JButton("Help", AllIcons.Actions.Help)

        bottomPanel.add(helpButton)
        helpButton.alignmentX = Component.RIGHT_ALIGNMENT
        val root = UI.PanelFactory.grid()
            .add(
                UI.PanelFactory.panel(inputText)
                    .withLabel("&Class Name:")
            )
            .add(
                UI.PanelFactory.panel(errorText)
            )
            .add(
                UI.PanelFactory.panel(JScrollPane(resultTable))
            )
            .add(
                UI.PanelFactory.panel(bottomPanel)
            )
            .createPanel()
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(root, inputText)
            .also {
                it.setFocusable(true)
                it.setRequestFocus(true)
                it.setResizable(true)
                it.setTitle("Search Artifacts")
                it.setCancelOnClickOutside(true)
                it.setMovable(true)
                it.setShowBorder(true)
                it.addListener(object : JBPopupListener {
                    override fun onClosed(event: LightweightWindowEvent) {
                        scope.cancel()
                    }
                })
            }
            .createPopup().apply {
                showCenteredInCurrentWindow(project)
            }

        popup.setMinimumSize(Dimension(800, 600))

        resultTable.getColumn(SearchResultTableModel.COL_ADD_DEPENDENCY).cellRenderer =
            ButtonRenderer(icon = AllIcons.General.Add)
        resultTable.getColumn(SearchResultTableModel.COL_ADD_DEPENDENCY).cellEditor =
            VersionPopupRenderer(
                project = project,
                currentModule = module
            ) {
                popup.cancel()
            }
        SearchWindowController(
            scope = scope,
            initialText = initialText,
            inputText = inputText,
            errorText = errorText,
            resultTable = resultTable,
            searchResultModel = searchResultModel,
            helpButton = helpButton,
            popup = popup
        ).startStream()
    }
}
