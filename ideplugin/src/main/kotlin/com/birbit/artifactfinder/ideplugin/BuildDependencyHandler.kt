package com.birbit.artifactfinder.ideplugin

import com.android.ide.common.repository.GradleCoordinate
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.projectsystem.getProjectSystem
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project


class BuildDependencyHandler(private val module: Module) {
    fun addMavenDependency(coordinate: String) {
        module.getModuleSystem().registerDependency(
            GradleCoordinate.parseCoordinateString(coordinate)
        )
        sync(module.project)
    }

    private fun sync(project: Project) {
        DataManager.getInstance().dataContextFromFocusAsync.onSuccess {
            val am = ActionManager.getInstance();
            val syncAction = am.getAction("Android.SyncProject")
            val fakeActionEvent = AnActionEvent(
                null, it,
                ActionPlaces.UNKNOWN, Presentation(),
                ActionManager.getInstance(), 0
            )
            syncAction.actionPerformed(fakeActionEvent)
        }
    }
}