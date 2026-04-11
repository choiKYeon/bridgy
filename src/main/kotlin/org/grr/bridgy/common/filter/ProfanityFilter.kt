package org.grr.bridgy.common.filter

import org.grr.bridgy.common.exception.CustomException
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ProfanityFilter {

    private val badWords: List<String> = ClassPathResource("profanity_list.txt")
        .inputStream
        .bufferedReader()
        .readLines()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("#") }

    fun check(text: String) {
        val normalized = text.lowercase().replace(" ", "")
        val found = badWords.firstOrNull { normalized.contains(it.lowercase()) }
        if (found != null) {
            throw CustomException("비속어가 포함된 내용은 작성할 수 없습니다.", HttpStatus.BAD_REQUEST)
        }
    }
}