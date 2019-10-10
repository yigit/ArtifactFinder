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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

class SearchArtifactIntentionAction : PsiElementBaseIntentionAction() {
    init {
        text = "Search Artifact"
    }
    override fun getFamilyName() = "Search Artifact"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return ModuleUtil.findModuleForPsiElement(element) != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val value = if (element is PsiWhiteSpace) {
            PsiTreeUtil.skipSiblingsBackward(element, PsiWhiteSpace::class.java)
                ?.text
        } else element.text
        SearchArtifactPanelController(
            project = project,
            module = ModuleUtil.findModuleForPsiElement(element),
            initialText = value?.split(" ")?.lastOrNull()
        ).buildAndShow()
    }
}
