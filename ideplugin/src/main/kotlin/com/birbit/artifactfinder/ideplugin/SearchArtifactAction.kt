package com.birbit.artifactfinder.ideplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class SearchArtifactAction : AnAction("Search Artifact") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        SearchArtifactPanelController(
            project = project
        ).buildAndShow()
    }
}

