# Adatvédelmi tájékoztató – Live Futár

Utolsó frissítés: 2026-09-24

## Milyen adatokat kezelünk?
Az app **nem gyűjt személyes adatokat** központi szerverre.

- **API-kulcs**: csak a készüléken, SharedPreferences-ben (a te Highlightly / soccer API kulcsod).
- **Kedvencek, téma, értesítés beállítás**: helyi tárolás.
- **Meccs-cache (Room + SharedPreferences)**: offline megjelenítéshez, a készüléken.
- **Értesítések**: Android Notification API; a tartalom a nyilvános meccsadatból jön.

## Hálózati forgalom
Az app a beállított focieredmény-API-t hívja (meccsek, odds, highlights).  
Nincs analitika-SDK, nincs reklám-SDK az alapcsomagban.

## Jogok
A helyi adatok törölhetők az app adatainak törlésével (Android beállítások).

## Kapcsolat
Ha kérdésed van: (e-mail cím).
