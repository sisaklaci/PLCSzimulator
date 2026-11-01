# PLC-szimulátor

Oktatási célú, **ingyenes** PLC (programozható logikai vezérlő) szimulátor,
amely négyféle, egymással szabadon kombinálható programozási nézettel mutatja
be a PLC-k működését: **létradiagram (LD)**, **funkcióblokk-diagram (FBD)**,
**szekvenciális funkcióséma (GRAFCET/SFC)** és egy szabadon tervezhető **HMI**
kezelőfelület. A program valós idejű, ciklikus (scan-alapú) kiértékeléssel
szimulálja a PLC működését, és a megírt logikát a legelterjedtebb ipari
gyártók gépi kód (utasításlista) formátumára is lefordítja.

Készítette: Sisak László ([helmetsoft.com](https://helmetsoft.com)) — INGYENES
oktatási célú felhasználásra, kereskedelmi forgalomba nem hozható.

## Futtatás

A program saját, beépített Java futtatókörnyezettel rendelkezik, külön
Java-telepítés nem szükséges.

1. Nyisd meg a [`dist/PlcSzimulator20261009`](dist/PlcSzimulator20261009) mappát.
2. Indítsd el a **`PlcSzimulator.exe`** fájlt (Windows).

Nincs szükség internetkapcsolatra, a program teljes mértékben helyben fut, és
futás közben nem küld/fogad adatot a hálózaton keresztül. A projektfájlok
**`.plc`** kiterjesztéssel menthetők/nyithatók meg.

### Fordítás forrásból

A teljes alkalmazás egyetlen fájlban van (`PlcSzimulator.java`, Swing GUI):

```
javac PlcSzimulator.java
java PlcSzimulator
```

## Főbb funkciók

- **Létradiagram szerkesztő**: érintkezők, tekercsek, Set/Reset, időzítők
  (TON), számlálók (CTU), párhuzamos ágak
- **Funkcióblokk-diagram (FBD)**: AND/OR/NOT/XOR, SR-tároló, TON, CTU,
  felfutó/lefutó élfigyelők (R_TRIG/F_TRIG)
- **GRAFCET (szekvenciális funkcióséma)**: lépések, átmenetek, receptivitások,
  akciók — és automatikus létraprogram-generálás a megtervezett szekvenciából
- **Óraimpulzusok (clock bitek)**: tetszőleges frekvenciájú, 50%-os
  kitöltésű villogó belső relék
- Szabadon tervezhető, több képernyős **HMI** felület, 12 elemtípussal,
  valós idejű animációval
- **Mintaprogramok könyvtára**: 9 kész, azonnal betölthető ipari és oktatási
  demonstráció
- **Gépi kód export**: IEC Instruction List, Siemens STL, Mitsubishi,
  Allen-Bradley
- Automatikus **programellenőrzés** és **kereszthivatkozás-keresés**
- **Nyomtatás és PDF export**, valamint teljes projektdokumentáció-generátor
- Többlépéses **Visszavonás/Ismét** a teljes projektre
- A felhasználói felület 6 nyelven: magyar, angol, német, spanyol, francia,
  lengyel

## Dokumentáció

Részletes felhasználói kézikönyv a [`kezikonyv/`](kezikonyv/) mappában,
6 nyelven (HU, EN, DE, FR, ES, PL), PDF formátumban.

## Mappaszerkezet

- [`PlcSzimulator.java`](PlcSzimulator.java) — a teljes alkalmazás forráskódja
- [`dist/`](dist/) — futtatható Windows-csomag (exe + bundlingolt Java runtime)
- [`kezikonyv/`](kezikonyv/) — felhasználói kézikönyv (PDF, 6 nyelven) és annak forrása
