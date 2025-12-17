package com.abhishek.limitedcart.order

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.abhishek.limitedcart.order",
        "com.abhishek.limitedcart.common"
    ]
)
class OrderServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<OrderServiceApplication>(*args)
        }
    }
}
