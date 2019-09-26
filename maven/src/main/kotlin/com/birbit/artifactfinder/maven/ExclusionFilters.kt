package com.birbit.artifactfinder.maven

private fun SUPPORT(pkg: String) = pkg.startsWith("com.android.support") ||
        pkg.startsWith("com.android.databinding")

private fun OLD_ARCH(pkg: String) = pkg.startsWith("android.arch")

private fun TOOLS(pkg: String) = pkg.startsWith("com.android.tools") ||
        pkg.startsWith("tools.base") || pkg == "zipflinger" || pkg.startsWith("com.android.java.tools")

private val PKG_EXCLUSION_FILTERS = listOf(::SUPPORT, ::OLD_ARCH, ::TOOLS)

@Suppress("UNUSED_PARAMETER")
private fun COMPILER(pkg: String, name: String) = name.contains("compiler")
@Suppress("UNUSED_PARAMETER")
private fun SIGNING(pkg: String, name: String) = name.contains("zipflinger") || name.contains("signflinger")
private val ARTFACT_EXCLUSION_FILTERS = listOf(::COMPILER, ::SIGNING)
internal fun shouldParsePackage(pkg: String) = PKG_EXCLUSION_FILTERS.none { it(pkg) }

internal fun shouldParseArtfiact(pkg: String, artifactName: String) =
    ARTFACT_EXCLUSION_FILTERS.none {
        it(pkg, artifactName)
    }

