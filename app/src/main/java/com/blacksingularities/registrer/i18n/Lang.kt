package com.blacksingularities.registrer.i18n

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Język interfejsu: zgodny z systemem albo wybrany w ustawieniach. */
enum class AppLanguage {
    SYSTEM,
    PL,
    EN,
    ;

    val label: String
        get() = when (this) {
            SYSTEM -> tr("Systemowy", "System")
            PL -> "Polski"
            EN -> "English"
        }
}

/**
 * Bieżący język. Stan Compose – zmiana języka od razu przebudowuje interfejs.
 * Polski dla systemu po polsku, w każdym innym przypadku angielski.
 */
object Lang {
    var polish by mutableStateOf(systemIsPolish(null))
        private set

    /** Język aplikacji wg systemu – z uwzględnieniem języka ustawionego dla samej aplikacji (Android 13+). */
    fun systemIsPolish(context: Context?): Boolean = runCatching {
        val config = context?.resources?.configuration ?: Resources.getSystem().configuration
        config.locales[0].language == "pl"
    }.getOrDefault(Locale.getDefault().language == "pl")

    fun apply(setting: AppLanguage, context: Context? = null) {
        polish = when (setting) {
            AppLanguage.SYSTEM -> systemIsPolish(context)
            AppLanguage.PL -> true
            AppLanguage.EN -> false
        }
    }

    /** Ustawienia regionalne do formatowania liczb i dat w interfejsie. */
    val locale: Locale get() = if (polish) Locale("pl", "PL") else Locale.US
}

/** Tekst w bieżącym języku interfejsu. */
fun tr(pl: String, en: String): String = if (Lang.polish) pl else en

/** Separator dziesiętny bieżącego języka (przecinek po polsku, kropka po angielsku). */
fun String.decimal(): String = if (Lang.polish) replace('.', ',') else this
