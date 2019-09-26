package com.birbit.build

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import java.io.File

object PublishUtil {
    @JvmStatic
    fun enablePublishing(
        proj:Any,
        artifactId: String
    ) {
        val project = proj as Project

        val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceTask = project.tasks.register("sourceJar", Jar::class.java) {
            it.classifier = "sources"
            it.from(javaPlugin.sourceSets.getByName("main").allSource)
        }

        project.plugins.apply("maven-publish")
        val ext = project.extensions.getByType(PublishingExtension::class.java)
        ext.repositories.maven {
            it.url = File(project.rootProject.buildDir, "repo").toURI()
        }
        ext.publications.create("maven", MavenPublication::class.java) {
            it.artifactId = artifactId
            it.groupId = "com.birbit.artifactfinder"
            it.version = "0.1"
            it.from(project.components.findByName("kotlin"))
            it.artifact(sourceTask.get())
        }

    }
}
