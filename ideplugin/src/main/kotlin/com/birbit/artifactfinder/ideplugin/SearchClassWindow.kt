package com.birbit.artifactfinder.ideplugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.FrameWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBPanel
import com.intellij.ui.layout.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.mozilla.javascript.tools.debugger.Dim
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class SearchArtifactPanelController(
    private val project: Project,
    private val module: Module? = null,
    private val initialText: String? = null
) {
    private val dependencyAdder = module?.let {
        AutoDependencyUtil(module)
    } ?: null
    private val job = SupervisorJob(null)
    private val scope = CoroutineScope(job)
    private val model = SearchArtifactModel()

    fun buildAndShow() {
        val searchResultModel = SearchResultTableModel(dependencyAdder != null)
        val resultTable = JBTable(searchResultModel).also {
            it.setMaxItemsForSizeCalculation(10)
            it.setShowColumns(true)
            it.autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
            it.fillsViewportHeight = true
        }

        resultTable.getColumn(SearchResultTableModel.COL_COPY).cellRenderer = CopyButtonRenderer("copy to clipboard")
        resultTable.getColumn(SearchResultTableModel.COL_COPY).cellEditor = CopyButtonEditor("Copied") { value ->
            (value as? SearchResult)?.let {
                val selection = StringSelection(it.qualifiedArtifact())
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            }
        }
        dependencyAdder?.let {
            resultTable.getColumn(SearchResultTableModel.COL_ADD_DEPENDENCY).cellRenderer =
                CopyButtonRenderer("add dependency")
            resultTable.getColumn(SearchResultTableModel.COL_ADD_DEPENDENCY).cellEditor =
                CopyButtonEditor("Added") { value ->
                    (value as? SearchResult)?.let {
                        dependencyAdder.addMavenDependency(it.qualifiedArtifact())
                    }
                }
        }

        val inputChannel = Channel<String>(Channel.CONFLATED)
        val directInputChannel = Channel<String>(Channel.CONFLATED)

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

        val inputText = JTextField(initialText)
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

        val root = UI.PanelFactory.grid()
            .add(
                UI.PanelFactory.panel(inputText.also {
                    it.addActionListener { actionEvent ->
                        println("action:$actionEvent")
                    }
                })
                    .withLabel("&Class Name:")
                    .withComment("input a class name. e.g: RecyclerView")
            )
            .add(UI.PanelFactory.panel(resultTable))
//            .add(UI.PanelFactory.panel(JButton("Help", AllIcons.Actions.Help)))
            .createPanel()

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(root, null)
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
                .consumeAsFlow()
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
                }.merge(directInputChannel.consumeAsFlow())
                .distinctUntilChanged()
                .mapLatest {
                    try {
                        withContext(Dispatchers.Main) {
                            resultTable.setPaintBusy(true)
                        }
                        model.query(it)
                    } catch (ioError: IOException) {
                        emptyList<SearchResult>()
                    } finally {
                        withContext(Dispatchers.Main) {
                            resultTable.setPaintBusy(false)
                        }
                    }
                }
                .collect {
                    searchResultModel.setItems(it)
                    root.preferredSize = root.preferredSize
                    root.revalidate()
                    popup.setMinimumSize(root.preferredSize)
                }
        }

    }

    companion object {
        private val DIMENSION_KEY = "SearchArtifactWindow"

    }
}

class SearchResultTableModel(
    private val supportsAddDependency: Boolean
) : DefaultTableModel(COLUMNS + if (supportsAddDependency) arrayOf(COL_ADD_DEPENDENCY) else emptyArray(), 0) {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return getColumnName(column) == COL_ADD_DEPENDENCY || getColumnName(column) == COL_COPY
     }

    fun setItems(items: List<SearchResult>) {
        (rowCount - 1 downTo 0).forEach {
            removeRow(it)
        }
        items.forEach { item ->
            addRow(
                arrayOf(
                    item.name, item.artifactDesc, item.version, item, item
                )
            )
        }
    }

    companion object {
        val COL_CLASS = "Class"
        val COL_ARTIFACT = "Artifact"
        val COL_VERSION = "Version"
        val COL_COPY = "Copy to clipboard"
        val COL_ADD_DEPENDENCY = "Add Dependency"

        val COLUMNS = arrayOf(COL_CLASS, COL_ARTIFACT, COL_VERSION, COL_COPY)
    }
}

private class CopyButtonEditor(val text: String,
                               val action: (Any?) -> Unit) : DefaultCellEditor(JCheckBox(text)) {
    override fun getTableCellEditorComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        return JButton(text).also {
            action(value)
        }
    }

}

private class CopyButtonRenderer(val text: String) : TableCellRenderer {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        return JButton(text).apply {
            isOpaque = true
            isEnabled = true
        }
    }
}


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