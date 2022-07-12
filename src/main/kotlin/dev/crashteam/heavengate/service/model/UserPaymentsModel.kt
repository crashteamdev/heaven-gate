package dev.crashteam.heavengate.service.model

data class UserPaymentsModel(
    val userPayments: List<UserPaymentModel>,
    val limit: Long,
    val offset: Long,
    val totalOffset: Long
)
