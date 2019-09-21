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
        implementation"org.jetbrains.kotlin:kotlin-stdlib-jdk7:${"$"}kotlin_version"
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