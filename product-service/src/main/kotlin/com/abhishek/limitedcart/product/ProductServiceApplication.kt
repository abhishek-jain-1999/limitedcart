package com.abhishek.limitedcart.product

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.abhishek.limitedcart.product",
        "com.abhishek.limitedcart.common"
    ]
)
class ProductServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<ProductServiceApplication>(*args)
        }
    }
}
