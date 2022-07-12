package dev.crashteam.heavengate.service.model

import java.time.LocalDateTime

data class UserPaymentModel(
    val paymentId: String,
    val status: String,
    val amount: Long,
    val createdAt: LocalDateTime
)
