package dev.portfolio.docversion.domain

import java.time.Instant

class Editor(private val versions: List<EditorUnit>) {

    fun asOf(t: Instant): List<EditorUnit> =
        versions.groupBy { it.key }
            .mapNotNull { (_, vs) ->
                vs.filter { it.validFrom <= t && (it.validTo == null || t < it.validTo) }
                    .maxByOrNull { it.validFrom }
            }
            .sortedBy { it.content }

    fun latest(): List<EditorUnit> =
        versions.filter { it.checkLatest() }.sortedBy { it.content }
}
