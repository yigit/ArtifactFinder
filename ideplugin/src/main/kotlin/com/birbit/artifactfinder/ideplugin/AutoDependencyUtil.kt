package com.birbit.artifactfinder.ideplugin

import com.android.ide.common.repository.GradleCoordinate
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.openapi.module.Module

class AutoDependencyUtil(private val module: Module) {
    fun addMavenDependency(coordinate: String) {
        module.getModuleSystem().registerDependency(
            GradleCoordinate.parseCoordinateString(coordinate)
        )
    }
}