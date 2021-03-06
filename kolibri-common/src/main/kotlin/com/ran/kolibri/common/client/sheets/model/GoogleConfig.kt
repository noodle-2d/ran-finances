package com.ran.kolibri.common.client.sheets.model

import com.typesafe.config.Config

data class GoogleConfig(
    val tokensPath: String,
    val accountsSpreadsheetId: String
) {
    constructor(config: Config) : this(
        config.getString("google.tokens-path"),
        config.getString("google.accounts-spreadsheet-id")
    )
}
