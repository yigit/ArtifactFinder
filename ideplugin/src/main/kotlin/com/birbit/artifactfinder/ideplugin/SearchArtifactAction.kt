package com.birbit.artifactfinder.ideplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.FrameWrapper
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer


class SearchArtifactAction : AnAction("Search Artifact") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val popup = PopupPanel()
        SearchClassWindow(project).also {
            it.init()
            it.show()
        }
//        JBPopupFactory.getInstance()
//            .createComponentPopupBuilder(panel, null)
//            .createPopup().showCenteredInCurrentWindow(project)
    }

    class PopupPanel : JBPanel<PopupPanel>(PopupLayout()) {

    }

    class PopupLayout() : GridBagLayout() {
        init {
            val constraints = GridBagConstraints()
            constraints.fill = GridBagConstraints.VERTICAL

            val input = JBTextField()
            constraints.gridx = 10
            constraints.gridy = 10
            constraints.weightx = 1.0
            addLayoutComponent(input, constraints)

        }
    }

    class SearchClassWindow(project: Project) : FrameWrapper(project, DIMENSION_KEY) {
        private val job = SupervisorJob(null)
        private val scope = CoroutineScope(job)
        private val model = SearchArtifactModel()

        init {
            setTitle("Search Artifacts")
            addDisposable {
                job.cancel()
            }
        }

        fun init() {
            val searchResultModel = SearchResultTableModel()
            val resultTable = JBTable(searchResultModel)

            resultTable.getColumn(SearchResultTableModel.COL_COPY).cellRenderer = CopyButtonRenderer("copy to clipboard")
            resultTable.getColumn(SearchResultTableModel.COL_COPY).cellEditor = CopyButtonEditor("Copied")

            val inputChannel = Channel<String>(Channel.CONFLATED)
            scope.launch {
                inputChannel
                    .consumeAsFlow()
                    .map {
                        it.trim()
                    }
                    .filter {
                        it != ""
                    }
                    .filter {
                        it.length > 2
                    }
                    .distinctUntilChanged()
                    .onEach {
                        delay(20)
                    }
                    .mapLatest {
                        try {
                            model.query(it)
                        } catch (ioError: IOException) {
                            emptyList<SearchResult>()
                        }
                    }
                    .collect {
                        searchResultModel.setItems(it)
                    }
            }


            val inputText = JTextField()

            fun maybeSearch(text: String) {
                scope.launch {
                    inputChannel.send(text)
                }
            }

            inputText.addActionListener {
                maybeSearch(inputText.text.toString())
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
                .createPanel()

            setComponent(root)
            frame.setSize(640, 640)
        }

        companion object {
            private val DIMENSION_KEY = "SearchArtifactWindow"

        }
    }

    class SearchResultTableModel() : DefaultTableModel(COLUMNS, 0) {
        fun addItem(item: String) {
            addRow(arrayOf(item, item, item, item))
        }

        override fun fireTableStructureChanged() {
            super.fireTableStructureChanged()
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return column == COLUMNS.size - 1
        }

        fun setItems(items: List<SearchResult>) {
            (rowCount - 1 downTo 0).forEach {
                removeRow(it)
            }
            items.forEach { item ->
                addRow(
                    arrayOf(
                        item.name, item.artifactDesc, item.version, item
                    )
                )
            }
        }

        companion object {
            val COL_CLASS = "Class"
            val COL_ARTIFACT = "Artifact"
            val COL_VERSION = "Version"
            val COL_COPY = "Copy to clipboard"

            val COLUMNS = arrayOf(COL_CLASS, COL_ARTIFACT, COL_VERSION, COL_COPY)
        }
    }

    private class CopyButtonEditor(val text:String) : DefaultCellEditor(JCheckBox(text)) {
        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            return JButton(text).also{
                (value as? SearchResult)?.let {
                    val selection = StringSelection(it.qualifiedArtifact())
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                }
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

}

