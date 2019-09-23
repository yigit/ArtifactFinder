package com.birbit.artifactfinder.parser.vo

import com.birbit.artifactfinder.parser.EXCLUSION_FILTERS

internal data class ArtifactInfoBuilder(
    private val classes: MutableSet<ParsedClassInfo> = mutableSetOf(),
    private val globalMethods: MutableSet<ParsedGlobalMethodInfo> = mutableSetOf(),
    private val extensionMethods: MutableSet<ParsedExtensionMethodInfo> = mutableSetOf(),
    private val innerClasses: MutableSet<InnerClassInfo> = mutableSetOf()
) {
    fun build(): ParsedArtifactInfo {
        val allAvailableClasses = mergeEligibleInnerClasses(classes, innerClasses)
        return ParsedArtifactInfo(
            classes = allAvailableClasses,
            globalMethods = globalMethods,
            extensionMethods = extensionMethods
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

    fun add(func: ParsedExtensionMethodInfo): ArtifactInfoBuilder {
        extensionMethods.add(func)
        return this
    }

    fun add(func: ParsedGlobalMethodInfo): ArtifactInfoBuilder {
        globalMethods.add(func)
        return this
    }
}