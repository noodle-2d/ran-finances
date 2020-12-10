package com.ran.kolibri.common.manager

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.ran.kolibri.common.client.telegram.TelegramClient
import com.ran.kolibri.common.client.telegram.model.SendMessageRequest
import com.ran.kolibri.common.client.telegram.model.TelegramConfig
import com.ran.kolibri.common.client.telegram.model.Update
import com.ran.kolibri.common.dao.TelegramIntegrationDao
import com.ran.kolibri.common.util.log

class TelegramManager(kodein: Kodein) {

    private val telegramClient: TelegramClient = kodein.instance()
    private val telegramConfig: TelegramConfig = kodein.instance()
    private val telegramIntegrationDao: TelegramIntegrationDao = kodein.instance()

    suspend fun sendMessageToOwner(text: String) {
        log.info("Sending message to owner: $text")
        val sendMessageRequest = SendMessageRequest(telegramConfig.botOwnerId, text)
        val telegramResponse = telegramClient.sendMessage(sendMessageRequest)
        log.info("Message was successfully sent. Response: $telegramResponse")
    }

    suspend fun pullUpdates() {
        val telegramIntegration = telegramIntegrationDao.get()
        val updatesResponse = telegramClient.getUpdates(telegramIntegration.lastUpdateId + 1)

        val updates = updatesResponse.result.orEmpty()
        updates.forEach { processUpdate(it) }

        updates.lastOrNull()?.updateId?.let { newLastUpdateId ->
            val updatedTelegramIntegration = telegramIntegration.copy(lastUpdateId = newLastUpdateId)
            telegramIntegrationDao.update(updatedTelegramIntegration)
            log.info("Processed ${updates.size} updates, last update id = $newLastUpdateId")
        }
    }

    private suspend fun processUpdate(update: Update) {
        log.info("Processing update $update")

        val chatId = update.message?.chat?.id
        if (chatId != telegramConfig.botOwnerId) {
            log.info("Ignoring message from chat $chatId")
            return
        }

        // todo: implement different functions here
        when (val text = update.message?.text.orEmpty()) {
            "/import-old-sheets" -> sendMessageToOwner("Importing old sheets is not supported yet")
            "/export-sheets" -> sendMessageToOwner("Exporting sheets is not supported yet")
            "/show-total-stat" -> sendMessageToOwner("Showing statistics is not supported yet")
            else -> log.info("Ignoring message with unknown text $text")
        }
    }
}
