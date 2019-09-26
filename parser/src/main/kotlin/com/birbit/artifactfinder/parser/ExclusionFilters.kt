package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.ParsedClassInfo

// exclude filters for class names

typealias ExclusionFilter = (ParsedClassInfo) -> Boolean

private fun BUILD_CONFIG(classInfo: ParsedClassInfo) = classInfo.name == "BuildConfig" ||
        classInfo.name == "R"
private fun LOWERCASE(classInfo: ParsedClassInfo) = classInfo.name[0].isLowerCase()
private fun COMPANION(classInfo: ParsedClassInfo) = classInfo.name.endsWith("Companion")
val EXCLUSION_FILTERS = listOf<ExclusionFilter>(::BUILD_CONFIG, ::LOWERCASE, ::COMPANION)