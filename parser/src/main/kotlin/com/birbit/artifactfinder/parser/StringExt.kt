package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.ClassInfo


internal fun String.toClassInfo() = split('/').let {
    if (it.isEmpty()) {
        throw IllegalArgumentException("there is no package name / class name in $this")
    } else if (it.size < 2) {
        ClassInfo(
            pkg = "",
            name = it[1].replace('.', '$') // for kotlin inner classes
        )
    } else {
        ClassInfo(
            pkg = it.take(it.size - 1).joinToString("."),
            name = it.last().replace('.', '$')
        )
    }
}