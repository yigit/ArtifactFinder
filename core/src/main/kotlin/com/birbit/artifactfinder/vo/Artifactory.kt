package com.birbit.artifactfinder.vo

// from: https://cwiki.apache.org/confluence/display/MAVEN/Remote+repository+layout
enum class Artifactory(
    val id: Int, // add an id so that we can persist them
    val baseUrl: String
) {
    GOOGLE(
        id = 0,
        baseUrl = "https://dl.google.com/dl/android/maven2/"
    ),
    MAVEN(
        id = 1,
        baseUrl = "https://repo1.maven.org/maven2/"
    );

    companion object {
        fun getById(id: Int): Artifactory {
            val artifactory = values().firstOrNull {
                it.id == id
            }
            checkNotNull(artifactory) {
                "invalid artifact id  $id"
            }
            return artifactory
        }
    }
}