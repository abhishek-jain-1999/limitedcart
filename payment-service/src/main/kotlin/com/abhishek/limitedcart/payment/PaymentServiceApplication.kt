package com.abhishek.limitedcart.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaymentServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<PaymentServiceApplication>(*args)
        }
    }
}
