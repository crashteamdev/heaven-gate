package dev.crashteam.heavengate.controller

import dev.crashteam.chest.wallet.WalletBlocking
import dev.crashteam.heavengate.service.WalletService
import dev.crashteam.openapi.heavengate.api.WalletsApi
import dev.crashteam.openapi.heavengate.model.AccountWallet
import dev.crashteam.openapi.heavengate.model.WalletBalance
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.security.Principal
import java.util.*

@RestController
class WalletsController(
    private val walletService: WalletService
) : WalletsApi {

    override fun createWallet(xRequestID: UUID, exchange: ServerWebExchange): Mono<ResponseEntity<AccountWallet>> {
        val walletCreateResponse = runBlocking {
            val principal = exchange.getPrincipal<Principal>().awaitSingle()
            walletService.createWallet(principal.name)
        } ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build<AccountWallet>().toMono()
        val accountWallet = AccountWallet().apply {
            this.id = walletCreateResponse.wallet.walletId
            this.balance = WalletBalance().apply {
                this.amount = walletCreateResponse.wallet.balance.amount
            }
            this.blocking = walletCreateResponse.wallet.blocking == WalletBlocking.BLOCKED
        }
        return ResponseEntity(accountWallet, HttpStatus.CREATED).toMono()
    }

    override fun getWallet(xRequestID: UUID?, exchange: ServerWebExchange): Mono<ResponseEntity<AccountWallet>> {
        val walletGetResponse = runBlocking {
            val principal = exchange.getPrincipal<Principal>().awaitSingle()
            walletService.getWalletByUser(principal.name)
        } ?: return ResponseEntity.notFound().build<AccountWallet>().toMono()
        val accountWallet = AccountWallet().apply {
            this.id = walletGetResponse.wallet.walletId
            this.balance = WalletBalance().apply {
                this.amount = walletGetResponse.wallet.balance.amount
            }
            this.blocking = walletGetResponse.wallet.blocking == WalletBlocking.BLOCKED
        }
        return ResponseEntity.ok(accountWallet).toMono()
    }
}
