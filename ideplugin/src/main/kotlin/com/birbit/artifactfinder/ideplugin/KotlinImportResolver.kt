package com.birbit.artifactfinder.ideplugin

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.quickfix.QuickFixContributor
import org.jetbrains.kotlin.idea.quickfix.QuickFixes

class KotlinImportResolver : QuickFixContributor {
    override fun registerQuickFixes(quickFixes: QuickFixes) {
        val action: IntentionAction = SearchArtifactIntentionAction()
        quickFixes.register(Errors.UNRESOLVED_REFERENCE, action)
    }
}