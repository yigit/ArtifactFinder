package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.ClassInfo

// exclude filters for class names

typealias ExclusionFilter = (ClassInfo) -> Boolean

private fun BUILD_CONFIG(classInfo: ClassInfo) = classInfo.name == "BuildConfig" ||
        classInfo.name == "R"
private fun LOWERCASE(classInfo: ClassInfo) = classInfo.name[0].isLowerCase()
val EXCLUSION_FILTERS = listOf<ExclusionFilter>(::BUILD_CONFIG, ::LOWERCASE)