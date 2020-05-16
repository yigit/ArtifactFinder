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

import com.birbit.artifactfinder.ideplugin.SearchResultModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.ui.TableUtil
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.AbstractTableCellEditor
import java.awt.Component
import java.awt.Point
import javax.swing.JTable

class VersionPopupRenderer(
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

        SelectDependencyDetailsWindow(
            project = project,
            module = currentModule,
            searchResult = result,
            onComplete = {
                TableUtil.stopEditing(table)
            }
        ).buildAndShow(RelativePoint(table, point))
    }
}
