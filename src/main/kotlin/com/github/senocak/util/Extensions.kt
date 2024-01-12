package com.github.senocak.util

import com.github.senocak.domain.User
import com.github.senocak.domain.Role
import com.github.senocak.domain.dto.RoleResponse
import com.github.senocak.domain.dto.UserResponse
import java.util.Date
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.StringUtils

/**
 * @return -- UserResponse object
 */
fun User.convertEntityToDto(roles: Boolean = false): UserResponse =
    UserResponse(
        name = this.name,
        email = this.email
    ).also {
        when {
            roles -> it.roles = this.roles.map { r: Role -> r.convertEntityToDto() }.toList()
        }
    }

/**
 * @return -- RoleResponse object
 */
fun Role.convertEntityToDto(): RoleResponse = RoleResponse(name = this.name!!)

/**
 * @return -- converted timestamp object that is long type
 */
private fun Date.toLong(): Long = this.time / 1000

/**
 * Split a string into two parts, separated by a delimiter.
 * @param delimiter The delimiter string
 * @return The array of two strings.
 */
fun String.split(delimiter: String): Array<String>? = StringUtils.split(this, delimiter)

fun <R : Any> R.logger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger((if (javaClass.kotlin.isCompanion) javaClass.enclosingClass else javaClass).name)
}
