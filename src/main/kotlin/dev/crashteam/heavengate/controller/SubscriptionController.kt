package dev.crashteam.heavengate.controller

import dev.crashteam.heavengate.service.SubscriptionService
import dev.crashteam.openapi.heavengate.api.SubscriptionsApi
import dev.crashteam.openapi.heavengate.model.ProductSubscription
import dev.crashteam.openapi.heavengate.model.Subscription
import dev.crashteam.subscription.ErrorGetSubscriptionResponse
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@RestController
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) : SubscriptionsApi {

    override fun getSubscription(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Subscription>> {
        return runBlocking {
            val subscription = subscriptionService.getSubscription(id.toString())
            if (subscription.hasErrorResponse()) {
                val responseEntity = when (subscription.errorResponse.code) {
                    ErrorGetSubscriptionResponse.ErrorCode.NOT_FOUND -> ResponseEntity.notFound().build()
                    ErrorGetSubscriptionResponse.ErrorCode.UNEXPECTED_ERROR,
                    ErrorGetSubscriptionResponse.ErrorCode.UNRECOGNIZED -> {
                        log.error {
                            "Error during get subscription. errorCode=${subscription.errorResponse.code};" +
                                    " errorDescription=${subscription.errorResponse.description}"
                        }
                        ResponseEntity.internalServerError().build<Subscription>()
                    }
                }
                return@runBlocking responseEntity
            }
            val subscriptionResponse = subscription.successResponse.subscription
            ResponseEntity.ok(Subscription().apply {
                this.id = UUID.fromString(subscriptionResponse.subscriptionId)
                this.name = subscriptionResponse.name
                this.description = subscriptionResponse.description
                this.level = subscriptionResponse.level
                this.price = subscriptionResponse.price
            })
        }.toMono()
    }

    override fun getSubscriptions(
        xRequestID: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<ProductSubscription>>> {
        return super.getSubscriptions(xRequestID, exchange)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
