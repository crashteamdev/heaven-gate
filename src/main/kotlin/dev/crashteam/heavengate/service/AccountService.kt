package dev.crashteam.heavengate.service

import dev.crashteam.account.AccountInfoRequest
import dev.crashteam.account.AccountInfoResponse
import dev.crashteam.account.AccountServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class AccountService {

    @GrpcClient("accountService")
    lateinit var accountGrpcService: AccountServiceGrpc.AccountServiceBlockingStub

    fun getAccount(userId: String): AccountInfoResponse {
        val accountInfoRequest = AccountInfoRequest.newBuilder().apply {
            this.userId = userId
        }.build()
        return accountGrpcService.getAccountInfo(accountInfoRequest)
    }

}
