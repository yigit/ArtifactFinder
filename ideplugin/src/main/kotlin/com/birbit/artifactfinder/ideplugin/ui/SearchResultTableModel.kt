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
import javax.swing.table.DefaultTableModel

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

        val COLUMNS = arrayOf(
            COL_CLASS,
            COL_ARTIFACT,
            COL_ADD_DEPENDENCY
        )
    }
}
