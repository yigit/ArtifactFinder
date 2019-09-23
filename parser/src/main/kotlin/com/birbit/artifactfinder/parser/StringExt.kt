package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.ParsedClassInfo


internal fun String.toClassInfo() = split('/').let {
    if (it.isEmpty()) {
        throw IllegalArgumentException("there is no package name / class name in $this")
    } else if (it.size < 2) {
        ParsedClassInfo(
            pkg = "",
            name = it[1].replace('.', '$') // for kotlin inner classes
        )
    } else {
        ParsedClassInfo(
            pkg = it.take(it.size - 1).joinToString("."),
            name = it.last().replace('.', '$')
        )
    }
}