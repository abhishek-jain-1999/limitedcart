package com.abhishek.limitedcart.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuthServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<AuthServiceApplication>(*args)
        }
    }
}
