package com.abhishek.limitedcart.inventory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.abhishek.limitedcart.inventory",
        "com.abhishek.limitedcart.common"
    ]
)
class InventoryServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<InventoryServiceApplication>(*args)
        }
    }
}
