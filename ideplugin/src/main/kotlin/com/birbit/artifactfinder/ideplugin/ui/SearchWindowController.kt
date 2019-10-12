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

import com.birbit.artifactfinder.ideplugin.SearchArtifactModel
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SearchWindowController(
    private val scope: CoroutineScope,
    private val inputText: JBTextField,
    private val errorText: JBLabel,
    private val helpButton: JButton,
    private val resultTable: JBTable,
    private val searchResultModel: SearchResultTableModel,
    private val popup: JBPopup,
    initialText: String? = null
) {
    private val logger = Logger.getInstance("artifact-finder")
    private val inputChannel = ConflatedBroadcastChannel<String>()
    private val directInputChannel = if (initialText == null) {
        ConflatedBroadcastChannel()
    } else {
        ConflatedBroadcastChannel(initialText)
    }
    private val model = SearchArtifactModel()

    init {
        errorText.isVisible = false
        helpButton.addActionListener {
            showHelp(helpButton)
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
    }

    fun startStream() {
        scope.launch {
            inputChannel
                .asFlow()
                .onEach {
                    delay(300)
                }.map {
                    it.trim()
                }.filter {
                    it != ""
                }.filter {
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
                    popup.content.preferredSize = popup.content.preferredSize
                    popup.content.revalidate()
                    popup.setMinimumSize(popup.content.preferredSize)
                    resizeColumnWidth(resultTable)
                }
        }
    }

    private fun doSearch(text: String) {
        scope.launch {
            directInputChannel.send(text)
        }
    }

    private fun maybeSearch(text: String) {
        scope.launch {
            inputChannel.send(text)
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

    private fun showHelp(helpButton: JButton) {
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                """
                            <p>
                            Artifact Finder allows you to find maven artifacts via class names and global or extension
                            names.
                            </p>
                            <p>
                            You can find the source code <a href="https://github.com/yigit/artifactFinder">here</a>.
                            If you want a certain artifact to be indexed, just edit <b>TBD</b> file in this link and
                            send a pull request.
                            </p>
                        """.trimIndent(),
                MessageType.INFO
            ) { linkEvent ->
                BrowserUtil.browse(linkEvent.url)
            }.createBalloon().showInCenterOf(helpButton as JComponent)
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