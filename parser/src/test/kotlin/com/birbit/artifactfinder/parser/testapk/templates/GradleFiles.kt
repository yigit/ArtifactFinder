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

package com.birbit.artifactfinder.parser.testapk.templates

fun settingsGradle() = """
    include ':lib'
""".trimIndent()

fun moduleBuildFile(
    hasKotlin: Boolean
): String {
    val applyKotlin = if (hasKotlin) {
        "apply plugin: 'kotlin-android'"
    } else {
        ""
    }
    return """
    apply plugin: 'com.android.library'
    $applyKotlin
    
    android {
        compileSdkVersion 29
        buildToolsVersion "29.0.2"
        defaultConfig {
            minSdkVersion 22
            targetSdkVersion 29
            versionCode 1
            versionName "1.0"
        }
    }
    
    dependencies {
        implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${"$"}kotlin_version"
        implementation "androidx.annotation:annotation:1.1.0"
    }

""".trimIndent()
}

fun mainBuildFile(
    agpVersion: String,
    kotlinVersion: String
) = """
    // Top-level build file where you can add configuration options common to all sub-projects/modules.

    buildscript {
        ext.kotlin_version = '$kotlinVersion'
        repositories {
            google()
            jcenter()
            
        }
        dependencies {
            classpath 'com.android.tools.build:gradle:$agpVersion'
            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        }
    }
    
    allprojects {
        repositories {
            google()
            jcenter()
        }
    }
    
    task clean(type: Delete) {
        delete rootProject.buildDir
    }

""".trimIndent()

fun gradlePropsFile(version: String) = """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
            distributionUrl=https\://services.gradle.org/distributions/gradle-$version-bin.zip
        """.trimIndent()
