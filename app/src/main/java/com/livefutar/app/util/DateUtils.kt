package com.livefutar.app.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A LIVE FUTÁR alkalmazás hivatalos időzónája.
 *
 * A Highlightly API alapértelmezésben UTC időzónát használ,
 * ezért az API lekérésnél és a megjelenítésnél is egységesen
 * Europe/Budapest időzónát használunk.
 */
private val HU_TIME_ZONE = TimeZone.getTimeZone("Europe/Budapest")

private val HU_LOCALE = Locale("hu", "HU")

object DateUtils {

    private val apiDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = HU_TIME_ZONE
        }

    /**
     * Mai dátum magyar idő szerint.
     */
    fun today(): String {
        return apiDateFormat.format(Date())
    }

    /**
     * Dátumsáv:
     * néhány nap visszamenőleg és több nap előre.
     *
     * A dátumok Europe/Budapest időzónában készülnek.
     */
    fun dateStrip(
        daysBefore: Int = 2,
        daysAfter: Int = 9
    ): List<String> {

        val result = mutableListOf<String>()

        val calendar = Calendar.getInstance(
            HU_TIME_ZONE,
            HU_LOCALE
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )
        calendar.set(
            Calendar.MINUTE,
            0
        )
        calendar.set(
            Calendar.SECOND,
            0
        )
        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        calendar.add(
            Calendar.DAY_OF_YEAR,
            -daysBefore
        )

        repeat(daysBefore + daysAfter + 1) {

            result.add(
                apiDateFormat.format(calendar.time)
            )

            calendar.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return result
    }

    /**
     * Rövid címke:
     *
     * Ma
     * Holnap
     * Tegnap
     * Szo 16.
     */
    fun shortChipLabel(
        dateStr: String
    ): String {

        val diff = daysFromToday(dateStr)
            ?: return dateStr

        return when (diff) {

            0 -> "Ma"

            1 -> "Holnap"

            -1 -> "Tegnap"

            else -> {

                val date = parseApiDate(dateStr)
                    ?: return dateStr

                val dayName =
                    SimpleDateFormat(
                        "EEE",
                        HU_LOCALE
                    ).format(date)

                val dayNumber =
                    SimpleDateFormat(
                        "d",
                        HU_LOCALE
                    ).format(date)

                "$dayName $dayNumber."
            }
        }
    }

    /**
     * Hosszú dátum:
     *
     * 2026. augusztus 14., péntek
     */
    fun fullDateLabel(
        dateStr: String
    ): String {

        val date = parseApiDate(dateStr)
            ?: return dateStr

        return SimpleDateFormat(
            "yyyy. MMMM d., EEEE",
            HU_LOCALE
        ).apply {
            timeZone = HU_TIME_ZONE
        }.format(date)
    }

    /**
     * Két dátum közötti napkülönbség
     * magyar idő szerint.
     */
    private fun daysFromToday(
        dateStr: String
    ): Int? {

        val target =
            parseApiDate(dateStr)
                ?: return null

        val todayCal =
            Calendar.getInstance(
                HU_TIME_ZONE,
                HU_LOCALE
            ).apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )
                set(
                    Calendar.MINUTE,
                    0
                )
                set(
                    Calendar.SECOND,
                    0
                )
                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val targetCal =
            Calendar.getInstance(
                HU_TIME_ZONE,
                HU_LOCALE
            ).apply {

                time = target

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )
                set(
                    Calendar.MINUTE,
                    0
                )
                set(
                    Calendar.SECOND,
                    0
                )
                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val diffMs =
            targetCal.timeInMillis -
                todayCal.timeInMillis

        return (
            diffMs /
                (24L * 60L * 60L * 1000L)
            ).toInt()
    }

    /**
     * API dátum -> Date.
     *
     * Kezeljük:
     *
     * 2026-08-14T18:00:00Z
     * 2026-08-14T18:00:00.000Z
     * 2026-08-14T20:00:00+02:00
     * 2026-08-14T20:00:00.000+02:00
     * 2026-08-14T20:00:00+0200
     * 2026-08-14T20:00:00
     */
    fun parseMatchDate(
        isoDateTime: String?
    ): Date? {

        if (isoDateTime.isNullOrBlank()) {
            return null
        }

        val patterns = listOf(

            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

            "yyyy-MM-dd'T'HH:mm:ssXXX",

            "yyyy-MM-dd'T'HH:mm:ss.SSSXX",

            "yyyy-MM-dd'T'HH:mm:ssXX",

            "yyyy-MM-dd'T'HH:mm:ss.SSSX",

            "yyyy-MM-dd'T'HH:mm:ssX",

            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

            "yyyy-MM-dd'T'HH:mm:ss'Z'",

            "yyyy-MM-dd'T'HH:mm:ss.SSS",

            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (pattern in patterns) {

            try {

                val parser =
                    SimpleDateFormat(
                        pattern,
                        Locale.US
                    )

                /*
                 * Ha nincs explicit időzóna a kapott dátumban,
                 * UTC-ként kezeljük.
                 *
                 * Az API hivatalos dátumai jellemzően ISO
                 * időzóna-információval érkeznek.
                 */
                if (
                    !pattern.contains("X") &&
                    !pattern.contains("'Z'")
                ) {
                    parser.timeZone =
                        TimeZone.getTimeZone("UTC")
                }

                if (
                    pattern.contains("'Z'")
                ) {
                    parser.timeZone =
                        TimeZone.getTimeZone("UTC")
                }

                val parsed =
                    parser.parse(isoDateTime)

                if (parsed != null) {
                    return parsed
                }

            } catch (
                _: ParseException
            ) {
                // Következő formátum.
            }
        }

        return null
    }

    /**
     * A meccs kezdési ideje magyar idő szerint.
     *
     * Például:
     *
     * API:
     * 2026-08-14T18:00:00.000Z
     *
     * Magyarország nyáron:
     * 20:00
     */
    fun kickoffTime(
        isoDateTime: String?
    ): String {

        val parsed =
            parseMatchDate(isoDateTime)
                ?: return "--:--"

        return SimpleDateFormat(
            "HH:mm",
            HU_LOCALE
        ).apply {
            timeZone = HU_TIME_ZONE
        }.format(parsed)
    }

    /**
     * A meccs kezdési időpontja milliszekundumban.
     *
     * Hasznos rendezéshez és annak eldöntéséhez,
     * hogy a kezdési idő már elmúlt-e.
     */
    fun kickoffMillis(
        isoDateTime: String?
    ): Long? {

        return parseMatchDate(
            isoDateTime
        )?.time
    }

    /**
     * Ellenőrzi, hogy a meccs kezdési időpontja
     * már elmúlt-e.
     */
    fun kickoffAlreadyPassed(
        isoDateTime: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {

        val kickoff =
            kickoffMillis(isoDateTime)
                ?: return false

        return kickoff <= nowMillis
    }

    /**
     * API YYYY-MM-DD dátum feldolgozása.
     */
    private fun parseApiDate(
        value: String
    ): Date? {

        return try {

            apiDateFormat.parse(value)

        } catch (
            _: ParseException
        ) {

            null
        }
    }
}
