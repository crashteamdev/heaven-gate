package dev.crashteam.heavengate.service

import dev.crashteam.chest.wallet.*
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class WalletService {

    @GrpcClient("walletService")
    lateinit var walletGrpcService: WalletServiceGrpc.WalletServiceBlockingStub

    fun createWallet(userId: String): WalletCreateResponse? {
        val walletCreateRequest = WalletCreateRequest.newBuilder().apply {
            this.account = AccountCreateRequest.newBuilder().apply {
                this.userId = userId
            }.build()
        }.build()
        return walletGrpcService.createWallet(walletCreateRequest)
    }

    fun getWalletByUser(userId: String): WalletGetResponse? {
        val walletGetRequest = WalletGetRequest.newBuilder().apply {
            this.userId = userId
        }.build()
        return walletGrpcService.getWallet(walletGetRequest)
    }

}
