package com.ran.kolibri.scheduler.manager.transaction

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.ran.kolibri.common.client.sheets.SheetsClient
import com.ran.kolibri.common.client.sheets.model.GoogleConfig
import com.ran.kolibri.common.client.sheets.model.SheetRange
import com.ran.kolibri.common.client.sheets.model.SheetRow
import com.ran.kolibri.common.entity.TelegramOperation
import com.ran.kolibri.common.manager.TelegramManager
import com.ran.kolibri.scheduler.manager.telegram.model.OperationUpdate

class AddFinancialAssetTransactionManager(kodein: Kodein) {

    private val telegramManager: TelegramManager = kodein.instance()

    private val googleConfig: GoogleConfig = kodein.instance()
    private val sheetsClient: SheetsClient = kodein.instance()

    suspend fun startAddingFinancialAssetTransaction(
        operation: TelegramOperation,
        update: OperationUpdate
    ): TelegramOperation {
        telegramManager.editMessageToOwner(operation.messageId!!, "Adding financial asset transaction. (todo)")
        return operation
    }

    private suspend fun updateSheets() {
        val range = "Тестинг!A1"
        val values = SheetRange(listOf(SheetRow(listOf("120,20", "340")), SheetRow(listOf("31.01.21", "780.09"))))
        sheetsClient.updateRange(googleConfig.accountsSpreadsheetId, range, values)
        telegramManager.sendMessageToOwner("Sheets updated")
    }

    private suspend fun appendSheets() {
        val range = "Тестинг!A1"
        val values = SheetRange(listOf(SheetRow(listOf("11", "33")), SheetRow(listOf("55", "77"))))
        sheetsClient.appendRange(googleConfig.accountsSpreadsheetId, range, values)
        telegramManager.sendMessageToOwner("Sheets appended")
    }
}
