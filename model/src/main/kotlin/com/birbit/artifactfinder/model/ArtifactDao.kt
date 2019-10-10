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

package com.birbit.artifactfinder.model

internal interface ArtifactDao {
    suspend fun insertArtifact(artifact: Artifact): Long

    suspend fun findArtifact(
        groupId: String,
        artifactId: String,
        version: Version
    ): Artifact?

    suspend fun insertClassRecord(classRecord: ClassRecord): Long

    suspend fun insertClassLookup(classLookup: ClassLookup)

    suspend fun searchClasses(query: String): List<SearchRecord>

    suspend fun searchMethods(query: String, methodSearchType: MethodSearchType): List<SearchRecord>

    suspend fun allLookups(): List<ClassLookup>

    suspend fun insertPendingArtifact(pendingArtifact: PendingArtifact)

    suspend fun findPendingArtifact(
        groupId: String,
        artifactId: String,
        version: Version
    ): Artifact?

    suspend fun incrementPendingArtifactRetry(id: Long)

    suspend fun markPendingArtifactFetched(id: Long)

    suspend fun findNextPendingArtifact(excludeIds: List<Long>): PendingArtifact?

    suspend fun deleteArtifact(artifact: Artifact)

    suspend fun deleteClassRecord(classRecord: ClassRecord)

    suspend fun insertMethodRecord(methodRecord: MethodRecord): Long

    suspend fun insertMethodLookup(methodLookup: MethodLookup)

    enum class MethodSearchType {
        ALL_METHOD,
        ONLY_EXTENSIONS,
        ONLY_GLOBAL;
        companion object {
            fun get(
                includeGlobal: Boolean,
                includeExtension: Boolean
            ): MethodSearchType? {
                return if (includeGlobal) {
                    if (includeExtension) {
                        ALL_METHOD
                    } else {
                        ONLY_GLOBAL
                    }
                } else if (includeExtension) {
                    ONLY_EXTENSIONS
                } else {
                    null
                }
            }
        }
    }
}
