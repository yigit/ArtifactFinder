/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.birbit.artifactfinder.model

import java.util.regex.Matcher
import java.util.regex.Pattern

data class Version(
    val major: Int,
    val minor: Int?,
    val patch: Int?,
    val extra: String?
) : Comparable<Version> {
    override fun compareTo(other: Version): Int = compareValuesBy(this, other,
        { it.major },
        { it.minor },
        { it.patch },
        { it.extra == null }, // False (no extra) sorts above true (has extra)
        { it.extra } // gradle uses lexicographic ordering
    )

    val isBeta = extra?.toLowerCase()?.startsWith("-beta") ?: false
    val isAlpha = extra?.toLowerCase()?.startsWith("-alpha") ?: false
    val isRc = extra?.toLowerCase()?.startsWith("-rc") ?: false
    val isRelease = extra == null

    override fun toString(): String {
        return "$major.$minor.$patch${extra ?: ""}"
    }

    companion object {
        private val VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(-.+)?$")
        fun fromString(input: String): Version? {
            val matcher = VERSION_REGEX.matcher(input)
            if (!matcher.matches()) {
                return null
            }

            return Version(
                major = matcher.group(1).toInt(),
                minor = matcher.safeGet(2)?.toInt(),
                patch = matcher.safeGet(3)?.toInt(),
                extra = matcher.safeGet(4)
            )
        }

        private fun Matcher.safeGet(index: Int): String? {
            return if (index <= groupCount()) {
                group(index)
            } else {
                null
            }
        }
    }
}
