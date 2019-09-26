package com.birbit.artifactfinder.parser.vo

/**
 * Parsed information about an artifact
 */
data class ParsedArtifactInfo(
    val classes: Set<ParsedClassInfo> = emptySet(),
    val globalMethods: Set<ParsedGlobalMethodInfo> = emptySet(),
    val extensionMethods: Set<ParsedExtensionMethodInfo> = emptySet()
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
 * Holds the data about a global kotlin method
 */
data class ParsedGlobalMethodInfo(
    val name: String
)

/**
 * Holds the data about an extension method in kotlin
 */
data class ParsedExtensionMethodInfo(
    val receiver: ParsedClassInfo,
    val name: String
)

/**
 * Holds the information about an inner class
 */
data class InnerClassInfo(
    val parent: ParsedClassInfo,
    val classInfo: ParsedClassInfo
)