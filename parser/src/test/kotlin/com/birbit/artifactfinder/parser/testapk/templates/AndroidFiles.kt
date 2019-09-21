package com.birbit.artifactfinder.parser.testapk.templates

fun androidManifest(
    appPkg: String
) = """
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="${appPkg}">
        <application
            android:allowBackup="false"
            android:label="foo"/>
    </manifest>

""".trimIndent()