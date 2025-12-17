package com.abhishek.limitedcart.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.abhishek.limitedcart.payment",
        "com.abhishek.limitedcart.common",
    ]
)
class PaymentServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<PaymentServiceApplication>(*args)
        }
    }
}
