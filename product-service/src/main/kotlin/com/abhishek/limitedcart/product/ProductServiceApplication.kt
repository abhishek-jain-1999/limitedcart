package com.abhishek.limitedcart.product

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProductServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<ProductServiceApplication>(*args)
        }
    }
}
