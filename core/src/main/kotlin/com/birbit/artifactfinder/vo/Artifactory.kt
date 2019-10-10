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
