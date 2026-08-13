package com.livefutar.app.util

object HungarianNames {

    private val countries = mapOf(
        "England" to "Anglia",
        "Spain" to "Spanyolorszag",
        "Italy" to "Olaszorszag",
        "Germany" to "Nemetorszag",
        "France" to "Franciaorszag",
        "Portugal" to "Portugal",
        "Netherlands" to "Hollandia",
        "Belgium" to "Belgium",
        "Turkey" to "Torokorszag",
        "Turkiye" to "Torokorszag",
        "Scotland" to "Skocia",
        "Wales" to "Wales",
        "Ireland" to "Irorszag",
        "Northern Ireland" to "Eszak-Irorszag",
        "Austria" to "Ausztria",
        "Switzerland" to "Svajc",
        "Poland" to "Lengyelorszag",
        "Czech Republic" to "Csehorszag",
        "Czechia" to "Csehorszag",
        "Slovakia" to "Szlovakia",
        "Hungary" to "Magyarorszag",
        "Romania" to "Romania",
        "Bulgaria" to "Bulgaria",
        "Serbia" to "Szerbia",
        "Croatia" to "Horvatorszag",
        "Slovenia" to "Szlovenia",
        "Bosnia and Herzegovina" to "Bosznia-Hercegovina",
        "Bosnia" to "Bosznia-Hercegovina",
        "Montenegro" to "Montenegro",
        "North Macedonia" to "Eszak-Macedónia",
        "Albania" to "Albania",
        "Greece" to "Gorogorszag",
        "Denmark" to "Dania",
        "Sweden" to "Svedorszag",
        "Norway" to "Norvegia",
        "Finland" to "Finnorszag",
        "Iceland" to "Izland",
        "Russia" to "Oroszorszag",
        "Ukraine" to "Ukrajna",
        "Belarus" to "Feheroroszorszag",
        "Georgia" to "Gruzia",
        "Armenia" to "Ormenyorszag",
        "Azerbaijan" to "Azerbajdzsan",
        "Kazakhstan" to "Kazahsztan",
        "Uzbekistan" to "Uzbegisztan",
        "Israel" to "Izrael",
        "Saudi Arabia" to "Szaud-Arabia",
        "United Arab Emirates" to "EAE",
        "UAE" to "EAE",
        "Qatar" to "Katar",
        "Egypt" to "Egyiptom",
        "Morocco" to "Marokko",
        "Tunisia" to "Tunezia",
        "Algeria" to "Algeria",
        "South Africa" to "Del-Afrika",
        "USA" to "USA",
        "United States" to "USA",
        "Mexico" to "Mexiko",
        "Canada" to "Kanada",
        "Brazil" to "Brazilia",
        "Argentina" to "Argentina",
        "Uruguay" to "Uruguay",
        "Chile" to "Chile",
        "Colombia" to "Kolumbia",
        "Japan" to "Japan",
        "South Korea" to "Del-Korea",
        "Korea Republic" to "Del-Korea",
        "China" to "Kina",
        "Australia" to "Ausztralia",
        "India" to "India",
        "World" to "Vilag",
        "Europe" to "Europa",
        "International" to "Nemzetkozi",
        "Latvia" to "Lettorszag",
        "Lithuania" to "Litvania",
        "Estonia" to "Esztorszag",
        "Moldova" to "Moldova",
        "Cyprus" to "Ciprus",
        "Kosovo" to "Koszovo"
    )

    private val leagues = mapOf(
        "Premier League" to "Premier League",
        "La Liga" to "La Liga",
        "Serie A" to "Serie A",
        "Bundesliga" to "Bundesliga",
        "Ligue 1" to "Ligue 1",
        "Championship" to "Championship",
        "FA Cup" to "FA Kupa",
        "EFL Cup" to "EFL Kupa",
        "Copa del Rey" to "Spanyol Kupa",
        "Coppa Italia" to "Olasz Kupa",
        "DFB Pokal" to "Nemet Kupa",
        "DFB-Pokal" to "Nemet Kupa",
        "Coupe de France" to "Francia Kupa",
        "Primeira Liga" to "Primeira Liga",
        "Eredivisie" to "Eredivisie",
        "Super Lig" to "Super Lig",
        "UEFA Champions League" to "Bajnokok Ligaja",
        "Champions League" to "Bajnokok Ligaja",
        "UEFA Europa League" to "Europa Liga",
        "Europa League" to "Europa Liga",
        "UEFA Europa Conference League" to "Konferencia Liga",
        "Europa Conference League" to "Konferencia Liga",
        "UEFA Super Cup" to "UEFA Szuperkupa",
        "UEFA Nations League" to "Nemzetek Ligaja",
        "FIFA World Cup" to "Vilagbajnoksag",
        "World Cup" to "Vilagbajnoksag",
        "NB I" to "NB I",
        "OTP Bank Liga" to "NB I",
        "NB II" to "NB II",
        "Magyar Kupa" to "Magyar Kupa",
        "MLS" to "MLS",
        "Saudi Pro League" to "Szaudi Pro League",
        "Pro League" to "Pro League",
        "Super League" to "Szuperliga",
        "1. HNL" to "1. HNL",
        "Ekstraklasa" to "Ekstraklasa",
        "Allsvenskan" to "Allsvenskan",
        "Eliteserien" to "Eliteserien"
    )

    fun country(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return countries[name]
            ?: countries.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
            ?: name
    }

    fun league(name: String?): String {
        if (name.isNullOrBlank()) return "Egyeb merkozesek"
        return leagues[name]
            ?: leagues.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
            ?: name
    }

    fun display(countryName: String?, leagueName: String?): String {
        val l = league(leagueName)
        val c = country(countryName).takeIf {
            it.isNotBlank() && !it.equals("Vilag", true) && !it.equals("World", true)
        }
        return if (c != null) c + " · " + l else l
    }
}
