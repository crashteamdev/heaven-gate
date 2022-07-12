package dev.crashteam.heavengate.controller

import dev.crashteam.account.ErrorAccountInfoResponse.ErrorCode
import dev.crashteam.heavengate.service.AccountService
import dev.crashteam.openapi.heavengate.api.AccountApi
import dev.crashteam.openapi.heavengate.model.*
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.security.Principal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@RestController
class AccountController(
    private val accountService: AccountService
) : AccountApi {

    override fun getAccount(xRequestID: UUID, exchange: ServerWebExchange): Mono<ResponseEntity<Account>> {
        return runBlocking {
            val principal = exchange.getPrincipal<Principal>().awaitSingle()
            val accountInfoResponse = accountService.getAccount(principal.name)
            if (accountInfoResponse.hasErrorResponse()) {
                val responseEntity = when (accountInfoResponse.errorResponse.code) {
                    ErrorCode.USER_NOT_FOUND -> ResponseEntity.notFound().build<Account>()
                    ErrorCode.UNEXPECTED_ERROR,
                    ErrorCode.UNRECOGNIZED -> {
                        val errorResponse = accountInfoResponse.errorResponse
                        log.error {
                            "Error during get account info. errorCode=${errorResponse.code};" +
                                    " errorDescription=${errorResponse.description}"
                        }
                        ResponseEntity.internalServerError().build()
                    }
                }
                return@runBlocking responseEntity
            }
            val successResponse = accountInfoResponse.successResponse
            val account = Account().apply {
                this.email = successResponse.account.email
                this.blocked = successResponse.account.blocked
                this.productSubscriptions = successResponse.account.productsList.map { accountProduct ->
                    AccountProductSubscriptionsInner().apply {
                        this.productId = UUID.fromString(accountProduct.product.productId)
                        this.subscriptionId = UUID.fromString(accountProduct.subscription.subscriptionId)
                        this.validUntil = LocalDateTime.ofEpochSecond(
                            accountProduct.validUntil.seconds,
                            accountProduct.validUntil.nanos,
                            ZoneOffset.UTC
                        ).atOffset(ZoneOffset.UTC)
                    }
                }
            }
            return@runBlocking ResponseEntity.ok(account)
        }.toMono()
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
