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

@file:Suppress("FunctionName")

package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.ParsedClassInfo
import com.birbit.artifactfinder.parser.vo.ParsedMethodInfo

// exclude filters for class names

typealias ExclusionFilter = (ParsedClassInfo) -> Boolean

typealias MethodExclusionFilter = (ParsedMethodInfo) -> Boolean

private fun BUILD_CONFIG(classInfo: ParsedClassInfo) = classInfo.name == "BuildConfig" ||
        classInfo.name == "R"
private fun LOWERCASE(classInfo: ParsedClassInfo) = classInfo.name[0].isLowerCase()
private fun COMPANION(classInfo: ParsedClassInfo) = classInfo.name.endsWith("Companion")
private fun DAGGER_COMPONENT(classInfo: ParsedClassInfo) = classInfo.name.startsWith("Dagger") &&
        classInfo.name.endsWith("Component")
private fun DEFAULT_IMPL(classInfo: ParsedClassInfo) = classInfo.name.endsWith("DefaultImpls")
private fun INTERNAL_PKG(classInfo: ParsedClassInfo) = classInfo.pkg.contains("internal")

private fun INTERNAL_METHOD_PKG(methodInfo: ParsedMethodInfo) = methodInfo.pkg.contains("internal")

val EXCLUSION_FILTERS = listOf<ExclusionFilter>(
    ::BUILD_CONFIG, ::LOWERCASE, ::COMPANION, ::DAGGER_COMPONENT, ::DEFAULT_IMPL, ::INTERNAL_PKG
)

val METHOD_EXCLUSION_FILTERS = listOf<MethodExclusionFilter>(
    ::INTERNAL_METHOD_PKG
)
