package com.birbit.artifactfinder

import com.birbit.artifactfinder.model.Version


object VersionSelector {
    /**
     * Current version selection logic:
     * Take last 3 stable releases
     * Take latest rc, if it is greater than latest stable
     * Take latest beta, if it is greater than latest stable & rc
     * Take latest alpha, if it is greater than latest stable & rc & beta
     *
     */
    fun selectVersions(input: List<Version>): Set<Version> {
        val sorted = input.sortedDescending()
        val selected = mutableListOf<Version>()

        val counters = Counters()
        sorted.forEach { version ->
            val select = if (version.isRelease) {
                true
            } else if (version.isRc) {
                selected.all {
                    if (it.isRelease) {
                        it < version
                    } else {
                        true // can pick rc when there is
                    }
                }
            } else if (version.isBeta) {
                selected.all {
                    if (it.isRelease || it.isRc) {
                        it < version
                    } else {
                        true
                    }
                }
            } else if (version.isAlpha) {
                selected.all {
                    if (it.isRelease || it.isRc || it.isBeta) {
                        it < version
                    } else {
                        true
                    }
                }
            } else {
                false
            }
            if (select && counters.inc(version)) {
                selected.add(version)
            }
        }
        return selected.toSet()
    }
}

private class Counter(
    var limit: Int,
    var cnt: Int = 0
) {
    fun inc(): Boolean {
        return if (cnt < limit) {
            cnt++
            true
        } else {
            false
        }
    }
}

private class Counters {
    var stable = Counter(LIMIT_STABLE)
    var rc = Counter(LIMIT_RC)
    var beta = Counter(LIMIT_BETA)
    var alpha = Counter(LIMIT_ALPHA)
    fun inc(version: Version): Boolean {
        val counter = when {
            version.isRelease -> stable
            version.isRc -> rc
            version.isBeta -> beta
            version.isAlpha -> alpha
            else -> return false
        }
        return counter.inc()
    }

    companion object {
        val LIMIT_STABLE = 3
        val LIMIT_RC = 1
        val LIMIT_BETA = 1
        val LIMIT_ALPHA = 1
    }
}