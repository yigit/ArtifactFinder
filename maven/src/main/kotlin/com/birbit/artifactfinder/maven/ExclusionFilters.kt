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

package com.birbit.artifactfinder.maven

private fun SUPPORT(pkg: String) = pkg.startsWith("com.android.support") ||
        pkg.startsWith("com.android.databinding")

private fun OLD_ARCH(pkg: String) = pkg.startsWith("android.arch")

private fun TOOLS(pkg: String) = pkg.startsWith("com.android.tools") ||
        pkg.startsWith("tools.base") || pkg == "zipflinger" || pkg.startsWith("com.android.java.tools")

private val PKG_EXCLUSION_FILTERS = listOf(::SUPPORT, ::OLD_ARCH, ::TOOLS)

@Suppress("UNUSED_PARAMETER")
private fun COMPILER(pkg: String, name: String) = name.contains("compiler")
@Suppress("UNUSED_PARAMETER")
private fun SIGNING(pkg: String, name: String) = name.contains("zipflinger") || name.contains("signflinger")
private val ARTFACT_EXCLUSION_FILTERS = listOf(::COMPILER, ::SIGNING)
internal fun shouldParsePackage(pkg: String) = PKG_EXCLUSION_FILTERS.none { it(pkg) }

internal fun shouldParseArtfiact(pkg: String, artifactName: String) =
    ARTFACT_EXCLUSION_FILTERS.none {
        it(pkg, artifactName)
    }
