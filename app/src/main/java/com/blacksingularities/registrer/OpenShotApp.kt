package com.blacksingularities.registrer

import android.app.Application
import com.blacksingularities.registrer.data.SettingsStore
import com.blacksingularities.registrer.i18n.Lang

/** Ustawia język interfejsu przed uruchomieniem ekranu, usług i kafelka. */
class OpenShotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { Lang.apply(SettingsStore(this).loadGlobal().language, this) }
    }
}
