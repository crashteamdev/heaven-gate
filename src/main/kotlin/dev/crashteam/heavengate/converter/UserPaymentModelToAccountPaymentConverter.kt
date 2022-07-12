package dev.crashteam.heavengate.converter

import dev.crashteam.heavengate.service.model.UserPaymentModel
import dev.crashteam.openapi.heavengate.model.AccountPayment
import dev.crashteam.openapi.heavengate.model.PaymentAmount
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class UserPaymentModelToAccountPaymentConverter : DataConverter<UserPaymentModel, AccountPayment> {

    override fun convert(source: UserPaymentModel): AccountPayment {
        return AccountPayment().apply {
            this.id = source.paymentId
            this.amount = PaymentAmount().apply {
                this.value = source.amount
            }
            this.status = AccountPayment.StatusEnum.valueOf(source.status.lowercase())
            this.createdAt = source.createdAt.atOffset(ZoneOffset.UTC)
        }
    }
}
