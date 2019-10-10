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
