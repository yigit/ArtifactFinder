package com.birbit.artifactfinder.model

import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
    val isRelease = extra == null

    override fun toString(): String {
        return "$major.$minor.$patch${extra ?: ""}"
    }

    companion object {
        private val VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(-.+)?$")
        fun fromString(input: String): Version? {
            val matcher = VERSION_REGEX.matcher(input)
            if (!matcher.matches()) {
                println("no match for $input")
                return null
            }
            val result = Version(
                major = matcher.group(1).toInt(),
                minor = matcher.safeGet(2)?.toInt(),
                patch = matcher.safeGet(3)?.toInt(),
                extra = matcher.safeGet(4)
            )
            println("parsed $input to $result")
            return result
        }

        private fun Matcher.safeGet(index: Int): String? {
            return if (index <= groupCount()) {
                group(index)
            } else {
                null
            }
        }
    }

    class RoomTypeConverter {
        @TypeConverter
        fun fromString(input: String) = Version.fromString(input)
        @TypeConverter
        fun convertToString(input: Version) = input.toString()
    }
}
