package dev.crashteam.heavengate.service

import dev.crashteam.subscription.*
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class SubscriptionService {

    @GrpcClient("subscriptionService")
    lateinit var subscriptionGrpcService: SubscriptionServiceGrpc.SubscriptionServiceBlockingStub

    fun getSubscription(subscriptionId: String): GetSubscriptionResponse {
        val subscriptionResponse = subscriptionGrpcService.getSubscription(GetSubscriptionRequest.newBuilder().apply {
            this.subscriptionId = subscriptionId
        }.build())
        return subscriptionResponse
    }

    fun getSubscriptions(productId: String): GetAllSubscriptionResponse {
        val subscriptionResponse = subscriptionGrpcService.getAllSubscription(GetAllSubscriptionRequest.newBuilder().apply {
            this.productId = productId
        }.build())
        return subscriptionResponse
    }

}
