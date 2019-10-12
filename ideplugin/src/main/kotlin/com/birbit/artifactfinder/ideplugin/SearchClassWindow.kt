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

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.SideBorder
import com.intellij.ui.TableUtil
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.AbstractTableCellEditor
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import com.intellij.openapi.diagnostic.Logger

@FlowPreview
@ExperimentalCoroutinesApi
class SearchArtifactPanelController(
    private val project: Project,
    private val module: Module? = null,
    private val initialText: String? = null
) {
    private val job = SupervisorJob(null)
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val model = SearchArtifactModel()
    private val logger = Logger.getInstance("artifact-finder")
    private lateinit var popup: JBPopup

    fun buildAndShow() {
        val searchResultModel = SearchResultTableModel()
        val resultTable = JBTable(searchResultModel).also {
            it.setMaxItemsForSizeCalculation(10)
            it.setShowColumns(true)
            it.autoResizeMode = JBTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            it.fillsViewportHeight = true
        }

        resultTable.getColumn(SearchResultTableModel.COL_ADD_DEPENDENCY).cellRenderer =
            ButtonRenderer(icon = AllIcons.General.Add)

        resultTable.getColumn(SearchResultTableModel.COL_ADD_DEPENDENCY).cellEditor = VersionPopupRenderer(
            project = project,
            currentModule = module
        ) {
            popup.cancel()
        }

        val inputChannel = ConflatedBroadcastChannel<String>()
        val directInputChannel = ConflatedBroadcastChannel<String>()

        fun doSearch(text: String) {
            scope.launch {
                directInputChannel.send(text)
            }
        }

        fun maybeSearch(text: String) {
            scope.launch {
                inputChannel.send(text)
            }
        }

        val inputText = JBTextField(initialText)
        inputText.emptyText.text = "input a class name. e.g: RecyclerView"
        val errorText = JBLabel(
            "", AllIcons.General.Error,JBLabel.LEADING)
        errorText.isVisible = false
        initialText?.let {
            doSearch(it)
        }

        inputText.addActionListener {
            doSearch(inputText.text)
        }

        inputText.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                maybeSearch(inputText.text)
            }

            override fun insertUpdate(e: DocumentEvent?) {
                maybeSearch(inputText.text)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                maybeSearch(inputText.text)
            }
        })

        resultTable.putClientProperty(UIUtil.KEEP_BORDER_SIDES, SideBorder.ALL)

        val bottomPanel = JPanel()
        bottomPanel.layout = BoxLayout(bottomPanel, BoxLayout.Y_AXIS)
        val helpButton = JButton("Help", AllIcons.Actions.Help)
        helpButton.addActionListener {
            JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(
                    """
                        Artifact Finder allows you to find maven artifacy via class names and global or extension names.
                        You can find the source code <a href="https://github.com/yigit/artifactFinder">here</a>.
                        If you want a certain artifact to be indexed, just edit <b>TBD</b> file in this link and
                        send a pull request.
                    """.trimIndent(),
                    MessageType.INFO
                ) { linkEvent ->
                    BrowserUtil.browse(linkEvent.url)
                }.createBalloon().showInCenterOf(helpButton as JComponent)
        }
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
            .add(UI.PanelFactory.panel(JScrollPane(resultTable)))
            .add(UI.PanelFactory.panel(bottomPanel))
            .createPanel()
        popup = JBPopupFactory.getInstance()
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
        scope.launch {
            inputChannel
                .asFlow()
                .onEach {
                    delay(300)
                }.map {
                    it.trim()
                }
                .filter {
                    it != ""
                }
                .filter {
                    it.length > 2
                }.merge(directInputChannel.asFlow())
                .distinctUntilChanged()
                .mapLatest {
                    try {
                        withContext(Dispatchers.Main) {
                            resultTable.setPaintBusy(true)
                        }
                        model.query(it)
                    } finally {
                        withContext(Dispatchers.Main) {
                            resultTable.setPaintBusy(false)
                        }
                    }
                }
                .retryWhen { cause, attempt ->
                    logger.error("Error while getting artifact search results", cause)
                    val msg = "${cause.javaClass.simpleName}: ${cause.localizedMessage ?: ""}"
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        errorText.text = msg
                        errorText.isVisible = true
                    }
                    // linear backoff
                    val delayTime = (attempt + 1) * 1_000
                    delay(delayTime.coerceAtMost(60_000))
                    true
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    errorText.isVisible = false
                    searchResultModel.setItems(it.items)
                    root.preferredSize = root.preferredSize
                    root.revalidate()
                    popup.setMinimumSize(root.preferredSize)
                    resizeColumnWidth(resultTable)
                }
        }
    }

    private fun resizeColumnWidth(table: JTable) {
        val columnModel = table.columnModel
        (0 until table.columnCount).forEach { column ->
            val maxWidth = (0 until table.rowCount).map { row ->
                val renderer = table.getCellRenderer(row, column)
                val comp = table.prepareRenderer(renderer, row, column)
                comp.preferredSize.width + 1
            }.max() ?: 100
            columnModel.getColumn(column).preferredWidth = maxWidth.coerceIn(5, 300)
        }
    }
}

class SearchResultTableModel : DefaultTableModel(COLUMNS, 0) {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return getColumnName(column) == COL_ADD_DEPENDENCY
    }

    fun setItems(items: List<SearchResultModel.SearchResult>) {
        (rowCount - 1 downTo 0).forEach {
            removeRow(it)
        }
        items.forEach { item ->
            addRow(
                arrayOf(
                    item.desc, item.artifactDesc, item
                )
            )
        }
    }

    companion object {
        private const val COL_CLASS = "Class / Method"
        private const val COL_ARTIFACT = "Artifact"
        const val COL_ADD_DEPENDENCY = "Add Dependency"

        val COLUMNS = arrayOf(COL_CLASS, COL_ARTIFACT, COL_ADD_DEPENDENCY)
    }
}

private class VersionPopupRenderer(
    private val project: Project,
    private val currentModule: Module?,
    private val onSuccess: () -> Unit
) : AbstractTableCellEditor() {
    private var editedValue: SearchResultModel.SearchResult? = null
    override fun getCellEditorValue(): Any {
        return editedValue ?: "?"
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        editedValue = (value as SearchResultModel.SearchResult)
        showVersionSelection(
            table = table,
            row = row,
            column = column,
            result = editedValue!!,
            project = project,
            currentModule = currentModule,
            onSelected = {
                onSuccess()
            }
        )
        return JBLabel(
            "select"
        )
    }

    fun showVersionSelection(
        table: JTable,
        row: Int,
        column: Int,
        result: SearchResultModel.SearchResult,
        project: Project,
        currentModule: Module?,
        onSelected: () -> Unit
    ) {
        val rect = table.getCellRect(row, column, true)
        val point = Point(rect.x, rect.y)
        VersionSelectionPopupController(
            result = result,
            project = project,
            currentModule = currentModule,
            callback = object : VersionSelectionPopupController.Callback {
                override fun onChosen() {
                    TableUtil.stopEditing(table)
                    onSelected()
                }

                override fun onCancel() {
                    TableUtil.stopEditing(table)
                }
            }
        ).buildPopup()
            .show(RelativePoint(table, point))
    }
}

private class ButtonRenderer(
    val text: String? = null,
    val icon: Icon? = null
) : TableCellRenderer {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        return JButton(text ?: "", icon).apply {
            isOpaque = true
            isEnabled = true
        }
    }
}

@ExperimentalCoroutinesApi
private fun <T> Flow<T>.merge(other: Flow<T>): Flow<T> {
    return channelFlow {
        val collectMe = launch {
            this@merge.collect {
                send(it)
            }
        }
        val collectOther = launch {
            other.collect {
                send(it)
            }
        }
        awaitClose {
            collectMe.cancel()
            collectOther.cancel()
        }
    }
}
