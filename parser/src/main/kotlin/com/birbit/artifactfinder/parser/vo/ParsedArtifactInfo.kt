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

import com.birbit.artifactfinder.parser.EXCLUSION_FILTERS

internal data class ArtifactInfoBuilder(
    private val classes: MutableSet<ParsedClassInfo> = mutableSetOf(),
    private val methods: MutableSet<ParsedMethodInfo> = mutableSetOf(),
    private val innerClasses: MutableSet<InnerClassInfo> = mutableSetOf()
) {
    fun build(): ParsedArtifactInfo {
        val allAvailableClasses = mergeEligibleInnerClasses(classes, innerClasses)
        return ParsedArtifactInfo(
            classes = allAvailableClasses,
            methods = methods
        )
    }

    fun add(classInfo: ParsedClassInfo): ArtifactInfoBuilder {
        if (EXCLUSION_FILTERS.none { it(classInfo) }) {
            classes.add(classInfo)
        }
        return this
    }

    fun add(innerClassInfo: InnerClassInfo): ArtifactInfoBuilder {
        if (EXCLUSION_FILTERS.none { it(innerClassInfo.classInfo) }) {
            innerClasses.add(innerClassInfo)
        }
        return this
    }

    /**
     * InnerClass's visibility depends on parent's visibility.
     * Traverse parents for inner classes to decide if they are truly visible.
     */
    private fun mergeEligibleInnerClasses(
        classes: Set<ParsedClassInfo>,
        innerClasses: Set<InnerClassInfo>
    ): Set<ParsedClassInfo> {
        val result = mutableSetOf<ParsedClassInfo>()
        result.addAll(classes)
        val candidates = mutableSetOf<InnerClassInfo>().also {
            it.addAll(innerClasses)
        }
        var startSize = result.size
        var endSize = -1
        while (candidates.isNotEmpty() && startSize != endSize) {
            startSize = result.size
            try {
                val hasVisibleParent = candidates.firstOrNull {
                    result.contains(it.parent)
                }
                hasVisibleParent?.let {
                    result.add(it.classInfo)
                    candidates.remove(it)
                }
            } finally {
                endSize = result.size
            }
        }
        return result
    }

    fun add(func: ParsedMethodInfo): ArtifactInfoBuilder {
        methods.add(func)
        return this
    }
}
