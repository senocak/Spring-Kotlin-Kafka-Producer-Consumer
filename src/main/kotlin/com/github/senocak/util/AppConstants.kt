package com.github.senocak.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.Normalizer
import java.util.regex.Pattern

object AppConstants {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    val corePoolSize: Int = Runtime.getRuntime().availableProcessors()
    const val DEFAULT_PAGE_NUMBER = "0"
    const val DEFAULT_PAGE_SIZE = "10"
    const val MAIL_REGEX = "^\\S+@\\S+\\.\\S+$"
    const val TOKEN_HEADER_NAME = "Authorization"
    const val TOKEN_PREFIX = "Bearer "
    const val ADMIN = "ADMIN"
    const val USER = "USER"
    const val securitySchemeName = "bearerAuth"

    /**
     * @param input -- string variable to make it sluggable
     * @return -- sluggable string variable
     */
    fun toSlug(input: String): String {
        val nonLatin: Pattern = Pattern.compile("[^\\w-]")
        val whiteSpace: Pattern = Pattern.compile("[\\s]")
        val noWhiteSpace: String = whiteSpace.matcher(input).replaceAll("-")
        val normalized: String = Normalizer.normalize(noWhiteSpace, Normalizer.Form.NFD)
        return nonLatin.matcher(normalized).replaceAll("")
    }
}