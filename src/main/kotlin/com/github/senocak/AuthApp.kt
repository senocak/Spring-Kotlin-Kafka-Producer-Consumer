package com.github.senocak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuthApp

fun main(args: Array<String>) {
    runApplication<AuthApp>(*args)
}
