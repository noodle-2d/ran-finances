package com.ran.kolibri.commandline.utility.service

import com.ran.kolibri.commandline.utility.dto.import.TransactionImportDto
import com.ran.kolibri.common.dto.sheets.SheetRow
import com.ran.kolibri.common.entity.Account
import com.ran.kolibri.common.entity.Transaction
import com.ran.kolibri.common.entity.enums.ExternalTransactionCategory
import com.ran.kolibri.common.entity.enums.TransactionType
import com.ran.kolibri.common.util.log
import java.math.BigDecimal
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object TransactionConverter : ConverterUtils {

    fun convertToImportDto(row: SheetRow): TransactionImportDto? {
        if (row.values[1] == "" && (row.values[3] == "Начальные" || row.values[3].startsWith("Закрытие"))) {
            log.info("Ignoring empty transaction row $row")
            return null
        }

        log.info("Converting transaction row $row")
        val accountString = row.values.first()
        val amount = bigDecimal(row.values[1])
        val resultAmount = bigDecimal(row.values[2])
        val comment = row.values[3]
        val date = DateTime.parse(row.values[4], DATE_FORMATTER)

        // todo: use it to evaluate "exact" fields
        val courseNumberPair = (if (row.values.size == 6) row.values[5] else null)
            ?.let { evaluateCourseNumber(it) }
        val courseCurrency = courseNumberPair?.first
        val courseNumber = courseNumberPair?.second

        return TransactionImportDto(
            accountString,
            evaluateTransactionType(accountString, comment),
            evaluateExternalTransactionCategory(accountString, comment),
            amount,
            resultAmount,
            date,
            comment,
            null,
            null,
            null
        )
    }

    // todo: implement it
    private fun evaluateTransactionType(accountString: String, comment: String): TransactionType =
        TransactionType.INCOME

    // todo: implement it
    private fun evaluateExternalTransactionCategory(
        accountString: String,
        comment: String
    ): ExternalTransactionCategory? =
        null

    private fun evaluateCourseNumber(courseString: String): Pair<String, BigDecimal>? =
        COURSE_NUMBER_REGEX.find(courseString)?.let { match ->
            Pair(match.groupValues[1], bigDecimal(match.groupValues[2]))
        }

    fun convert(importDto: TransactionImportDto, accounts: List<Account>): Transaction {
        val accountId = accounts
            .find { importDto.accountString.contains(it.name) }
            ?.id!!
        return Transaction(
            accountId = accountId,
            type = importDto.type,
            externalTransactionCategory = importDto.externalTransactionCategory,
            amount = importDto.amount,
            date = importDto.date,
            comment = importDto.comment,
            associatedTransactionId = null,
            exactFinancialAssetPrice = importDto.exactFinancialAssetPrice,
            exactBoughtCurrencyRatioPart = importDto.exactBoughtCurrencyRatioPart,
            exactSoldCurrencyRatioPart = importDto.exactSoldCurrencyRatioPart
        )
    }

    private val DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.yy")
    private val COURSE_NUMBER_REGEX = Regex("""^\(курс 1(.*) [=~] ([0-9,]*).*\)$""")
}
