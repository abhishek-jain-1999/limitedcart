package com.abhishek.limitedcart.payment.service

import com.abhishek.limitedcart.payment.dto.CardDetails
import com.abhishek.limitedcart.payment.dto.PaymentResult
import java.math.BigDecimal
import java.util.UUID

interface PaymentProcessor {
    fun charge(amount: BigDecimal, cardDetails: CardDetails): PaymentResult
    fun refund(paymentId: UUID): PaymentResult
}
