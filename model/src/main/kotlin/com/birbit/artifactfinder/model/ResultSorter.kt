package com.birbit.artifactfinder.model

import java.util.Locale

object ResultSorter {
    fun sort(query:String, results : List<SearchRecord>) : List<SearchRecord> {
        val original = query
        val lowercased = original.toLowerCase(Locale.US)
        results.forEach {
            it.score = score(
                original = original,
                lowercased = lowercased,
                record = it
            )
        }
        return results.sorted()
    }

    private fun score(original: String,
                      lowercased:String,
                      record:SearchRecord) : Int {
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
        lowercasedClassName : String
    ) : Int {
        var penalty = 0
        val startPos = lowercasedClassName.indexOf(lowercased)
        if (startPos < 0) { //safe guard, should not happen in regular search
            return 50 + className.length
        }
        if (startPos != 0 && className[startPos - 1] != '$') {
            penalty += startPos
        }

        // ever lowercase / uppercase mismatch is also minus 1
        for(offset in 0 until original.length) {
            if(original[offset] != className[startPos + offset]) {
                penalty ++
            }
        }
        var endPos = startPos + lowercased.length
        while(endPos < className.length) {
            penalty += 1
            endPos ++
        }

        return penalty
    }

    private val MAX_SCORE = 100
}