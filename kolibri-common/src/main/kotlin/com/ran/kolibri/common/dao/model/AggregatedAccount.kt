package com.ran.kolibri.common.dao.model

import com.ran.kolibri.common.entity.enums.AccountType
import com.ran.kolibri.common.entity.enums.Currency
import com.ran.kolibri.common.entity.enums.FinancialAssetType
import java.math.BigDecimal
import org.joda.time.DateTime

data class AggregatedAccount(
    val accountId: Long,
    val accountName: String,
    val currentAmount: BigDecimal,
    val accountType: AccountType,
    val accountCurrency: Currency?,
    val accountCreateDate: DateTime,
    val financialAssetType: FinancialAssetType?,
    val financialAssetCurrency: Currency?
)
