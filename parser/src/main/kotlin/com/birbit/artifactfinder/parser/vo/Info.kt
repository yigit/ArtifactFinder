package com.birbit.artifactfinder.parser.vo

/**
 * Holds the data about a class
 */
data class ClassInfo(
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
data class GlobalMethodInfo(
    val name: String
)

/**
 * Holds the data about an extension method in kotlin
 */
data class ExtensionMethodInfo(
    val receiver: ClassInfo,
    val name: String
)

/**
 * Holds the information about an inner class
 */
data class InnerClassInfo(
    val parent: ClassInfo,
    val classInfo: ClassInfo
)