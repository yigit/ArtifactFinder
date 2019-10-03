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
            PsiTreeUtil.skipSiblingsBackward(element, PsiWhiteSpace::class.java)?.text
        } else element.text
        SearchArtifactPanelController(
            project = project,
            module = ModuleUtil.findModuleForPsiElement(element),
            initialText = value
        ).buildAndShow()

    }
}