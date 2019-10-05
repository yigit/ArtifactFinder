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