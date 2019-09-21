package com.birbit.artifactfinder.parser.vo

import com.birbit.artifactfinder.parser.EXCLUSION_FILTERS

/**
 * Parsed information about an artifact
 */
data class ArtifactInfo(
    val classes: Set<ClassInfo>,
    val globalMethods: Set<GlobalMethodInfo>,
    val extensionMethods: Set<ExtensionMethodInfo>
)

internal data class ArtifactInfoBuilder(
    private val classes: MutableSet<ClassInfo> = mutableSetOf(),
    private val globalMethods: MutableSet<GlobalMethodInfo> = mutableSetOf(),
    private val extensionMethods: MutableSet<ExtensionMethodInfo> = mutableSetOf(),
    private val innerClasses: MutableSet<InnerClassInfo> = mutableSetOf()
) {
    fun build(): ArtifactInfo {
        val allAvailableClasses = mergeEligibleInnerClasses(classes, innerClasses)
        return ArtifactInfo(
            classes = allAvailableClasses,
            globalMethods = globalMethods,
            extensionMethods = extensionMethods
        )
    }

    fun add(classInfo: ClassInfo): ArtifactInfoBuilder {
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
        classes: Set<ClassInfo>,
        innerClasses: Set<InnerClassInfo>
    ): Set<ClassInfo> {
        val result = mutableSetOf<ClassInfo>()
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

    fun add(func: ExtensionMethodInfo): ArtifactInfoBuilder {
        extensionMethods.add(func)
        return this
    }

    fun add(func: GlobalMethodInfo): ArtifactInfoBuilder {
        globalMethods.add(func)
        return this
    }
}