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

import java.util.*

object ResultSorter {
    fun sort(query: String, results: List<SearchRecord>): List<SearchRecord> {
        val lowercased = query.toLowerCase(Locale.US)
        results.forEach {
            it.score = score(
                original = query,
                lowercased = lowercased,
                record = it
            )
        }
        return results.sorted()
    }

    private fun score(
        original: String,
        lowercased: String,
        record: SearchRecord
    ): Int {
        var score = MAX_SCORE
        if (original == record.name) {
            return score
        }
        score -= 10
        val lowercasedClassName = record.name.toLowerCase(Locale.US)
        if (lowercased == lowercasedClassName) {
            return score
        }
        score -= similarityPenalty(
            original,
            lowercased,
            className = record.name,
            lowercasedClassName = lowercasedClassName
        )
        return score
    }

    private fun similarityPenalty(
        original: String,
        lowercased: String,
        className: String,
        lowercasedClassName: String
    ): Int {
        var penalty = 0
        val startPos = lowercasedClassName.indexOf(lowercased)
        if (startPos < 0) { // safe guard, should not happen in regular search
            return 50 + className.length
        }
        if (startPos != 0 && className[startPos - 1] != '$') {
            penalty += startPos
        }

        // ever lowercase / uppercase mismatch is also minus 1
        for (offset in 0 until original.length) {
            if (original[offset] != className[startPos + offset]) {
                penalty++
            }
        }
        var endPos = startPos + lowercased.length
        while (endPos < className.length) {
            penalty += 1
            endPos++
        }

        return penalty
    }

    private val MAX_SCORE = 100
}
