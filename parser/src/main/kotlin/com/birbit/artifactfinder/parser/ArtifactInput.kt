package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.ClassZipEntry
import com.birbit.artifactfinder.parser.vo.CodeSource
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Represents a jar artifact
 */
class Jar(
    private val inputStream: InputStream
) : CodeSource {
    override val classDeclarations
        get() = ZipInputStream(inputStream).asClassFileSequence()
}

/**
 * Represents an AAR artifact
 */
class Aar(
    private val inputStream: InputStream
) : CodeSource {
    override val classDeclarations
        get() : Sequence<ClassZipEntry> {
            val zip = ZipInputStream(inputStream)
            val classesJar = zip.asEntrySequence().first {
                it.name == "classes.jar"
            }.let {
                zip
            }
            return ZipInputStream(classesJar).asClassFileSequence()
        }
}

private fun ZipInputStream.asClassFileSequence(): Sequence<ClassZipEntry> {
    return sequence {
        this@asClassFileSequence.use {
            var next = nextEntry
            while (next != null) {
                if (next.name.endsWith(".class")) {
                    val bytes = this@asClassFileSequence.readBytes()

                    yield(
                        ClassZipEntry(
                            entry = next,
                            stream = bytes
                        )
                    )
                }
                next = try {
                    nextEntry
                } catch (io: IOException) {
                    if (io.message == "Stream closed") {
                        null
                    } else {
                        throw io
                    }
                }
            }
        }
    }
}

private fun ZipInputStream.asEntrySequence(): Sequence<ZipEntry> {
    return sequence {
        this@asEntrySequence.use {
            var next = nextEntry
            while (next != null) {
                yield(next)
                next = nextEntry
            }
        }
    }
}