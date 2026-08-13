package com.livefutar.app.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Minden dátumos szöveg explicit magyar nyelvi beállítással készül, hogy a
// hét napjai / hónapnevek magyarul jelenjenek meg attól függetlenül, hogy
// a telefon rendszernyelve mire van állítva.
private val HU_LOCALE = Locale("hu", "HU")

object DateUtils {

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun today(): String = apiDateFormat.format(Date())

    /**
     * Egy dátumsáv (pl. a Kezdőképernyő tetején) opciói: néhány nap visszamenőleg
     * és előre, "yyyy-MM-dd" formátumban - ez megy egyenesen az API date paraméterébe.
     */
    fun dateStrip(daysBefore: Int = 2, daysAfter: Int = 9): List<String> {
        val result = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysBefore)
        repeat(daysBefore + daysAfter + 1) {
            result.add(apiDateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    /** Rövid címke egy dátumsáv-chiphez: "MA", "HOLNAP", vagy pl. "Szo 16." */
    fun shortChipLabel(dateStr: String): String {
        val diff = daysFromToday(dateStr) ?: return dateStr
        return when (diff) {
            0 -> "Ma"
            1 -> "Holnap"
            -1 -> "Tegnap"
            else -> {
                val date = apiDateFormat.parse(dateStr) ?: return dateStr
                val dayName = SimpleDateFormat("EEE", HU_LOCALE).format(date)
                val dayNumber = SimpleDateFormat("d", HU_LOCALE).format(date)
                "$dayName $dayNumber."
            }
        }
    }

    /** Hosszú, teljes dátumcím a kiválasztott naphoz: "2026. augusztus 13., csütörtök" */
    fun fullDateLabel(dateStr: String): String {
        val date = apiDateFormat.parse(dateStr) ?: return dateStr
        return SimpleDateFormat("yyyy. MMMM d., EEEE", HU_LOCALE).format(date)
    }

    private fun daysFromToday(dateStr: String): Int? {
        val target = apiDateFormat.parse(dateStr) ?: return null
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            time = target
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diffMs = targetCal.timeInMillis - todayCal.timeInMillis
        return (diffMs / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * A meccs kezdési idejét (API-tól kapott ISO 8601 dátum-idő) a telefon
     * helyi időzónájában, "HH:mm" formában adja vissza. Több lehetséges
     * bemeneti formátumot is megpróbál, mert az API néha eltérően küldi.
     */
    fun kickoffTime(isoDateTime: String?): String {
        if (isoDateTime.isNullOrBlank()) return "--:--"
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsedDate = parser.parse(isoDateTime) ?: continue
                return SimpleDateFormat("HH:mm", HU_LOCALE).format(parsedDate)
            } catch (e: ParseException) {
                // Próbáljuk a következő mintát
            }
        }
        return "--:--"
    }
}
