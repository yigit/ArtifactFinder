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

package com.birbit.artifactfinder.parser.vo

/**
 * Parsed information about an artifact
 */
data class ParsedArtifactInfo(
    val classes: Set<ParsedClassInfo> = emptySet(),
    val methods: Set<ParsedMethodInfo> = emptySet()
)

/**
 * Holds the data about a class
 */
data class ParsedClassInfo(
    /**
     * Package for the class. Parts separated by '.'
     */
    val pkg: String,
    /**
     * name of the class. Parts, if exists, separated by '$'
     */
    val name: String
)

/**
 * Holds the data about an extension methods in kotlin or global methods
 */
data class ParsedMethodInfo(
    val pkg: String,
    // null for global methods
    val receiver: ParsedClassInfo?,
    val name: String
)

/**
 * Holds the information about an inner class
 */
data class InnerClassInfo(
    val parent: ParsedClassInfo,
    val classInfo: ParsedClassInfo
)
