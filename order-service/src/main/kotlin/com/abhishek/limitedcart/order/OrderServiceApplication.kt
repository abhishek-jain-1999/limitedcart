package com.abhishek.limitedcart.order

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrderServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<OrderServiceApplication>(*args)
        }
    }
}
