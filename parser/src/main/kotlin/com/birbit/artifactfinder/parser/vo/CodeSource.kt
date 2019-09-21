package com.birbit.artifactfinder.parser.vo

import java.util.zip.ZipEntry

class ClassZipEntry(
    val entry: ZipEntry,
    val stream: ByteArray
)

/**
 * Some artifact that can provide class information
 */
interface CodeSource {
    val classDeclarations: Sequence<ClassZipEntry>
}