package com.birbit.artifactfinder.parser

import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.objectweb.asm.tree.AnnotationNode

/**
 * Extracts KotlinClassMetadata from a Metadata annotation
 */
@Suppress("UNCHECKED_CAST")
internal fun AnnotationNode.extractKotlinMetadata(): KotlinClassMetadata? {
    desc ?: return null
    val values = values ?: return null
    var k: Int? = null
    var mv: ArrayList<Int>? = null
    var bv: ArrayList<Int>? = null
    var d1: ArrayList<String>? = null
    var d2: ArrayList<String>? = null

    var key: String? = null
    values.forEach {
        if (key == null) {
            key = it as String
        } else {
            when (key) {
                "k" -> k = it as Int
                "mv" -> mv = it as ArrayList<Int>
                "bv" -> bv = it as ArrayList<Int>
                "d1" -> d1 = it as ArrayList<String>
                "d2" -> d2 = it as ArrayList<String>
                else -> {
                    throw IllegalArgumentException("unknown key: $key")
                }
            }
            key = null
        }
    }

    val header = KotlinClassHeader(
        kind = k!!,
        metadataVersion = mv!!.toIntArray(),
        bytecodeVersion = bv!!.toIntArray(),
        data1 = d1!!.toTypedArray(),
        data2 = d2!!.toTypedArray(),
        extraString = null,
        extraInt = null,
        packageName = null
    )
    return KotlinClassMetadata.read(header)
}