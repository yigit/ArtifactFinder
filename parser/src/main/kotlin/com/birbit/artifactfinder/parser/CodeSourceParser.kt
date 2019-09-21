package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.ArtifactInfo
import com.birbit.artifactfinder.parser.vo.ArtifactInfoBuilder
import com.birbit.artifactfinder.parser.vo.CodeSource

/**
 * Parses a [CodeSource] and turns it into [ArtifactInfo]
 */
object CodeSourceParser {
    fun parse(source: CodeSource): ArtifactInfo {
        val builder = ArtifactInfoBuilder()
        source.classDeclarations.forEach {
            ClassZipEntryParser.parse(it, builder)
        }
        return builder.build()
    }
}