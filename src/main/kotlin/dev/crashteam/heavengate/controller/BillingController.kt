package dev.crashteam.heavengate.controller

import dev.crashteam.heavengate.service.PaymentService
import dev.crashteam.heavengate.service.model.UserPaymentsModel
import dev.crashteam.openapi.heavengate.api.BillingApi
import dev.crashteam.openapi.heavengate.model.AccountPayment
import dev.crashteam.openapi.heavengate.model.CreatePaymentRequest
import dev.crashteam.openapi.heavengate.model.PaymentAmount
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.core.convert.ConversionService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.security.Principal
import java.time.ZoneOffset
import java.util.*

@RestController
class BillingController(
    private val paymentService: PaymentService,
    private val conversionService: ConversionService,
) : BillingApi {

    override fun createPayment(
        xRequestID: UUID,
        idempotencyKey: UUID,
        createPaymentRequest: Mono<CreatePaymentRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<AccountPayment>> {
        val accountPayment = runBlocking {
            val paymentRequest = createPaymentRequest.awaitSingle()
            val userPayment = paymentService.createPayment(
                idempotencyKey = idempotencyKey,
                amount = paymentRequest.amount.value,
                currencyCode = paymentRequest.amount.currencyCode,
                description = paymentRequest.description,
                returnUrl = paymentRequest.returnUrl
            )
            conversionService.convert(userPayment, AccountPayment::class.java)
        }
        return ResponseEntity(accountPayment, HttpStatus.CREATED).toMono()
    }

    override fun getPayment(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<AccountPayment>> {
        return runBlocking {
            val userPaymentModel = paymentService.getPayment(
                paymentId = id.toString()
            ) ?: return@runBlocking ResponseEntity.notFound().build<AccountPayment>()
            val accountPayment = AccountPayment().apply {
                this.id = id.toString()
                this.amount = PaymentAmount().apply {
                    this.value = userPaymentModel.amount
                }
                this.status = AccountPayment.StatusEnum.valueOf(userPaymentModel.status.lowercase())
                this.createdAt = userPaymentModel.createdAt.atOffset(ZoneOffset.UTC)
            }
            return@runBlocking ResponseEntity(accountPayment, HttpStatus.OK)
        }.toMono()
    }

    override fun getPayments(
        xRequestID: UUID,
        limit: Int,
        offset: Int,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<AccountPayment>>> {
        return runBlocking {
            val principal = exchange.getPrincipal<Principal>().awaitSingle()
            val userPayments: UserPaymentsModel = paymentService.getPayments(principal.name, limit, offset)
                ?: return@runBlocking ResponseEntity.notFound().build<Flux<AccountPayment>>()
            val accountPayments = userPayments.userPayments.map {
                AccountPayment().apply {
                    this.id = it.paymentId
                    this.amount = PaymentAmount().apply {
                        this.value = it.amount
                    }
                    this.status = AccountPayment.StatusEnum.valueOf(it.status.lowercase())
                    this.createdAt = it.createdAt.atOffset(ZoneOffset.UTC)
                }
            }.toFlux()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", userPayments.totalOffset.toString())
                add("Pagination-Limit", userPayments.limit.toString())
                add("Pagination-Offset", userPayments.offset.toString())
            }
            return@runBlocking ResponseEntity(accountPayments, httpHeaders, HttpStatus.OK)
        }.toMono()
    }
}
