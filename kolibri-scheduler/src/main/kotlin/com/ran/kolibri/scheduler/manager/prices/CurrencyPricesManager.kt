package com.ran.kolibri.scheduler.manager.prices

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.ran.kolibri.common.client.open.exchange.rates.OpenExchangeRatesClient
import com.ran.kolibri.common.dao.CurrencyPriceDao
import com.ran.kolibri.common.entity.CurrencyPrice
import com.ran.kolibri.common.entity.enums.Currency
import com.ran.kolibri.common.manager.TelegramManager
import com.ran.kolibri.common.util.log
import com.ran.kolibri.scheduler.manager.TelegramBotNotifyingUtils
import java.lang.IllegalStateException
import org.joda.time.DateTime

class CurrencyPricesManager(kodein: Kodein) : TelegramBotNotifyingUtils {

    private val currencyPriceDao: CurrencyPriceDao = kodein.instance()
    private val openExchangeRatesClient: OpenExchangeRatesClient = kodein.instance()

    override val telegramManager: TelegramManager = kodein.instance()

    suspend fun updateCurrencyPricesWithNotification() =
        doActionSendingMessageToOwner("updating currency prices") { updateCurrencyPrices() }

    suspend fun updateCurrencyPrices(): String {
        val storedCurrencyDates = getStoredCurrencyDates()
        val datesList = buildDatesList()

        datesList.forEach { date ->
            if (isNeededToRequestForDate(date, storedCurrencyDates)) {
                log.info("Requesting currency prices for date $date")
                val dateCurrencyPrices = requestCurrencyPricesForDate(date)
                log.info("Storing currency prices for date $date: $dateCurrencyPrices")
                storeCurrencyPrices(dateCurrencyPrices, storedCurrencyDates)
            }
        }

        return "Updated currency prices"
    }

    private suspend fun getStoredCurrencyDates(): Map<Long, List<Currency>> =
        currencyPriceDao.selectAll()
            .groupBy { it.date.millis }
            .mapValues { currencyEntry -> currencyEntry.value.map { it.currency } }

    private fun buildDatesList(): List<DateTime> {
        val daysQuantity = (DateTime().withTimeAtStartOfDay().millis - WATCH_START_DATE.millis) / MILLIS_IN_DAY
        return (0..daysQuantity.toInt()).map { day -> WATCH_START_DATE.plusDays(day) }
    }

    private fun isNeededToRequestForDate(date: DateTime, storedCurrencyDates: Map<Long, List<Currency>>): Boolean {
        val dateCurrencies = storedCurrencyDates[date.millis] ?: listOf()
        return !dateCurrencies.containsAll(WATCHED_CURRENCIES)
    }

    private suspend fun requestCurrencyPricesForDate(date: DateTime): List<CurrencyPrice> {
        val currencyRates = if (isToday(date)) openExchangeRatesClient.getLatestRates()
        else openExchangeRatesClient.getHistoricalRates(date)
        val ratesMap = currencyRates.rates ?: mapOf()

        return WATCHED_CURRENCIES.map { currency ->
            CurrencyPrice(
                currency = currency,
                baseCurrency = currencyRates.base ?: emptyFieldError("base"),
                price = ratesMap[currency.name] ?: emptyFieldError("rates.${currency.name}"),
                date = date
            )
        }
    }

    private fun isToday(date: DateTime): Boolean =
        date.isBeforeNow && date.plusDays(1).isAfterNow

    private suspend fun storeCurrencyPrices(
        currencyPrices: List<CurrencyPrice>,
        storedCurrencyDates: Map<Long, List<Currency>>
    ) {
        val dateCurrencies = storedCurrencyDates[currencyPrices.first().date.millis] ?: listOf()
        val currencyPricesToStore = currencyPrices.filter { !dateCurrencies.contains(it.currency) }
        currencyPriceDao.insert(currencyPricesToStore)
    }

    private fun emptyFieldError(field: String): Nothing =
        throw IllegalStateException("Field $field is empty")

    companion object {
        private val WATCH_START_DATE = DateTime.parse("2018-01-01T00:00:00Z")
        private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000
        private val WATCHED_CURRENCIES = listOf(
            Currency.RUB,
            Currency.EUR,
            Currency.CNY,
            Currency.GBP,
            Currency.CZK,
            Currency.BTC
        )
    }
}