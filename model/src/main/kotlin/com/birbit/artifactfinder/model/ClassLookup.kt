package com.birbit.artifactfinder.model

/**
 * Represents the lookup information for a class.
 *
 * For instance, if the class is called "Foo$Bar", we'll two entries in this table that has
 * identifier "Bar" and "Foo$Bar" that refers back to the classId.
 *
 * Indentifiers are always lowercase so that we can do fast lookup w/ case-insensitive like
 *
 * The identifier is indexed, which allows us to do a query on it with prefix-search
 */
data class ClassLookup(
    val identifier:String,
    val classId: Long
)