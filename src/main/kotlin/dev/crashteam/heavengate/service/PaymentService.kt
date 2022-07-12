package dev.crashteam.heavengate.service

import dev.crashteam.heavengate.service.model.UserPaymentModel
import dev.crashteam.heavengate.service.model.UserPaymentsModel
import dev.crashteam.payment.*
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class PaymentService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {

    @GrpcClient("paymentService")
    lateinit var paymentGrpcService: PaymentServiceGrpc.PaymentServiceBlockingStub

    suspend fun createPayment(
        idempotencyKey: UUID,
        amount: Long,
        currencyCode: String,
        description: String,
        returnUrl: String
    ): UserPaymentModel {
        val paymentId = UUID.randomUUID().toString()
        val paymentCreateRequest = PaymentCreateRequest.newBuilder().apply {
            this.id = UUID.randomUUID().toString()
            this.amount = Amount.newBuilder().apply {
                this.value = amount
                this.currency = currencyCode
            }.build()
            this.description = description
            this.returnUrl = returnUrl
        }.build()
        val cachedPaymentId = redisTemplate.opsForValue().get(idempotencyKey.toString()).awaitSingleOrNull()
        if (cachedPaymentId != null) {
            val userPaymentResponse: PaymentResponse =
                paymentGrpcService.getPayment(PaymentQuery.newBuilder().apply {
                    this.paymentId = cachedPaymentId
                }.build())
            return UserPaymentModel(
                paymentId = userPaymentResponse.userPayment.paymentId,
                status = userPaymentResponse.userPayment.status.name,
                amount = userPaymentResponse.userPayment.amount.value,
                createdAt = userPaymentResponse.userPayment.createdAt.let {
                    LocalDateTime.ofEpochSecond(it.seconds, it.nanos, ZoneOffset.UTC)
                }
            )
        }
        redisTemplate.opsForValue().set(idempotencyKey.toString(), paymentId, Duration.of(24, ChronoUnit.HOURS))
            .awaitSingle()
        val paymentCreateResponse = paymentGrpcService.createPayment(paymentCreateRequest)

        return UserPaymentModel(
            paymentId = paymentCreateResponse.paymentId,
            status = paymentCreateResponse.status.name,
            amount = paymentCreateResponse.amount.value,
            createdAt = paymentCreateResponse.createdAt.let {
                LocalDateTime.ofEpochSecond(it.seconds, it.nanos, ZoneOffset.UTC)
            }
        )
    }

    suspend fun getPayment(paymentId: String): UserPaymentModel? {
        val userPaymentResponse = paymentGrpcService.getPayment(PaymentQuery.newBuilder().apply {
            this.paymentId = paymentId
        }.build()) ?: return null

        return UserPaymentModel(
            paymentId = userPaymentResponse.userPayment.paymentId,
            status = userPaymentResponse.userPayment.status.name,
            amount = userPaymentResponse.userPayment.amount.value,
            createdAt = userPaymentResponse.userPayment.createdAt.let {
                LocalDateTime.ofEpochSecond(it.seconds, it.nanos, ZoneOffset.UTC)
            }
        )
    }

    suspend fun getPayments(userId: String, limit: Int, offset: Int): UserPaymentsModel? {
        val paymentsResponse = paymentGrpcService.getPayments(PaymentsQuery.newBuilder().apply {
            this.userId = userId
            this.pagination = LimitOffsetPagination.newBuilder().apply {
                this.limit = limit.toLong()
                this.offset = offset.toLong()
            }.build()
        }.build()) ?: return null
        val userPayments = paymentsResponse.userPaymentList.map { payment ->
            UserPaymentModel(
                paymentId = payment.paymentId,
                status = payment.status.name,
                amount = payment.amount.value,
                createdAt = payment.createdAt.let {
                    LocalDateTime.ofEpochSecond(it.seconds, it.nanos, ZoneOffset.UTC)
                }
            )
        }
        return UserPaymentsModel(
            userPayments = userPayments,
            limit = paymentsResponse.pagination.limit,
            offset = paymentsResponse.pagination.offset,
            totalOffset = paymentsResponse.pagination.totalOffset
        )
    }

}
