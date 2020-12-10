package com.ran.kolibri.scheduler.kodein

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.provider
import com.ran.kolibri.scheduler.watcher.OldSheetsImportWatcher
import com.ran.kolibri.scheduler.watcher.SheetsExportWatcher
import com.ran.kolibri.scheduler.watcher.TelegramPullWatcher

val watchersModule = Kodein.Module {
    bind<OldSheetsImportWatcher>() with provider { OldSheetsImportWatcher() }
    bind<SheetsExportWatcher>() with provider { SheetsExportWatcher() }
    bind<TelegramPullWatcher>() with provider { TelegramPullWatcher() }
}