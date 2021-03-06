package com.ran.kolibri.scheduler.manager.importing

import com.ran.kolibri.common.client.sheets.model.SheetRow
import com.ran.kolibri.common.entity.Account
import com.ran.kolibri.common.entity.Transaction
import com.ran.kolibri.common.entity.enums.ExternalTransactionCategory
import com.ran.kolibri.common.entity.enums.TransactionType
import com.ran.kolibri.common.util.log
import com.ran.kolibri.scheduler.manager.importing.model.TransactionImportDto
import java.lang.RuntimeException
import java.math.BigDecimal
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object TransactionConverter : ConverterUtils {

    fun convertToImportDto(row: SheetRow): TransactionImportDto {
        log.info("Converting transaction row $row")
        val accountString = row.values.first()
        val resultAmount = bigDecimal(row.values[2])
        val amount = evaluateAmount(row.values[1], resultAmount)
        val comment = row.values[3]
        val date = DateTime.parse(row.values[4], DATE_FORMATTER)
        val transactionType = evaluateTransactionType(comment, amount)

        val (exactFinancialAssetPrice, exactSoldCurrencyRatioPart, exactBoughtCurrencyRatioPart) =
            (if (row.values.size == 6) row.values[5] else null)
                ?.let { evaluateExactCourseValues(it, comment) }
                ?: Triple(null, null, null)

        return TransactionImportDto(
            accountString,
            transactionType,
            evaluateExternalTransactionCategory(accountString, comment, transactionType),
            amount,
            resultAmount,
            date,
            comment,
            exactFinancialAssetPrice,
            exactSoldCurrencyRatioPart,
            exactBoughtCurrencyRatioPart
        )
    }

    private fun evaluateAmount(amountString: String, resultAmount: BigDecimal): BigDecimal =
        if (amountString == "") resultAmount else bigDecimal(amountString)

    private fun evaluateTransactionType(comment: String, amount: BigDecimal): TransactionType =
        when {
            contains(comment, FINANCIAL_ASSET_SINGS) && contains(comment, PURCHASE_SIGNS) ->
                TransactionType.FINANCIAL_ASSET_PURCHASE
            contains(comment, FINANCIAL_ASSET_SINGS) && contains(comment, SALE_SIGNS) ->
                TransactionType.FINANCIAL_ASSET_SALE
            contains(comment, CURRENCY_CONVERSION_SIGNS) && !contains(comment, NOT_CURRENCY_CONVERSION_SIGNS) ->
                TransactionType.CURRENCY_CONVERSION
            contains(comment, TRANSFER_SIGNS) ->
                TransactionType.TRANSFER
            amount > BigDecimal.ZERO && !contains(comment, INCOME_EXCLUSIONS) ->
                TransactionType.INCOME
            else ->
                TransactionType.EXPENSE
        }

    // todo: implement it
    private fun evaluateExternalTransactionCategory(
        accountString: String,
        comment: String,
        transactionType: TransactionType
    ): ExternalTransactionCategory? =
        null

    private fun evaluateExactCourseValues(
        courseString: String,
        comment: String
    ): Triple<BigDecimal?, BigDecimal?, BigDecimal?>? =
        COURSE_NUMBER_REGEX.find(courseString)?.let { match ->
            val courseCurrency = match.groupValues[1]
            val courseNumber = bigDecimal(match.groupValues[2])
            log.info("Course currency: $courseCurrency; course number: $courseNumber")
            validateExactCourseValues(courseCurrency, courseNumber, comment)

            when (courseCurrency) {
                "шт." -> Triple(courseNumber, null, null)
                else -> when {
                    contains(comment, PURCHASE_SIGNS) -> Triple(null, courseNumber, BigDecimal.ONE)
                    contains(comment, SALE_SIGNS) -> Triple(null, BigDecimal.ONE, courseNumber)
                    else ->
                        throw RuntimeException("Strange currency conversion: $courseCurrency, $courseNumber, $comment")
                }
            }
        }

    private fun validateExactCourseValues(
        courseCurrency: String,
        courseNumber: BigDecimal,
        comment: String
    ) {
        when (courseCurrency.trim()) {
            "шт." -> if (!contains(comment, FINANCIAL_ASSET_SINGS)) {
                throw RuntimeException("Strange financial asset course: $courseCurrency, $courseNumber, $comment")
            }
            "$", "E", "Е", "U", "B", "крона" -> if (!contains(comment, CURRENCY_CONVERSION_SIGNS)) {
                throw RuntimeException("Strange currency conversion values: $courseCurrency, $courseNumber, $comment")
            }
            else -> throw RuntimeException("Strange exact course values: $courseCurrency, $courseNumber, $comment")
        }
    }

    fun convert(importDto: TransactionImportDto, accounts: List<Account>): Transaction? {
        if (importDto.amount == BigDecimal.ZERO) {
            if (importDto.comment == "Начальные" || importDto.comment?.startsWith("Закрытие") == true) {
                log.info("Ignoring zero transaction $importDto")
                return null
            } else {
                log.info("Strange transaction $importDto")
                throw RuntimeException("Strange transaction $importDto")
            }
        }

        val accountId = accounts
            .find { importDto.accountString.endsWith(it.name) }
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
    private val FINANCIAL_ASSET_SINGS = setOf("акц", "облигац", "офз")
    private val CURRENCY_CONVERSION_SIGNS = setOf("доллар", "евро", "юаней", "чешских крон", "биткоин")
    private val NOT_CURRENCY_CONVERSION_SIGNS = setOf("евроторг")
    private val TRANSFER_SIGNS = setOf(
        "перевод",
        "снятие",
        "отдано в долг",
        "возврат долга",
        "вывод",
        "внесение денег",
        "пополнение брокерского"
    )
    private val INCOME_EXCLUSIONS = setOf("возврат в wildberries")

    private val SALE_SIGNS = setOf(
        "продажа",
        "перевод долларов в рубли",
        "перевод евро в рубли",
        "перевод биткоинов в рубли"
    )

    private val PURCHASE_SIGNS = setOf(
        "покупка",
        "перевод рублей в доллары",
        "перевод рублей в евро",
        "перевод рублей в биткоины"
    )
}
