# OpenShot

**Aparat, kamera i dyktafon w jednej aplikacji na Androida** – z RAW/DNG, obliczeniowym trybem nocnym, fuzją HDR, ręcznym trybem Pro, płynnym zoomem i studiem dźwięku.

[![Build & Release APK](../../actions/workflows/android.yml/badge.svg)](../../actions/workflows/android.yml)

## Pobieranie

Najnowszy plik APK jest zawsze w **[Releases → Latest](../../releases/latest)**. Każdy push do `main` buduje i publikuje nową wersję automatycznie.

1. Pobierz `OpenShot-x.y.z.apk` na telefon.
2. Otwórz plik i zezwól na instalację z tego źródła.
3. Wymagany Android 10 (API 29) lub nowszy.

## Funkcje

### Zdjęcia
| Tryb | Co robi |
|---|---|
| **Noc** | Sprzętowy tryb nocny producenta (CameraX Extensions), a gdy go brak – własny potok: seria 4–12 klatek, wyrównanie, uśrednianie z odrzucaniem „duchów” i adaptacyjne mapowanie tonów. |
| **Portret** | Sprzętowe rozmycie tła (Bokeh), jeśli telefon je udostępnia. |
| **Zdjęcie** | Szybka migawka lub priorytet jakości, lampa auto/wł./latarka, samowyzwalacz 3/10 s. |
| **Zdjęcie ruchome** | Zdjęcie oraz około 3 s ruchu przed i po naciśnięciu migawki. |
| **HDR** | Sprzętowy HDR albo fuzja 3 ekspozycji (−1,7 / 0 / +1,7 EV) z lokalnym mapowaniem tonów. |
| **Długi czas** | Seria klatek z 1–15 s łączona w jedno zdjęcie: rozmycie ruchu (średnia) albo smugi światła (maksimum), z wyrównaniem do zdjęć z ręki. |
| **Tryb ręczny** | Fotograficzny tryb ręczny: ISO, czas naświetlania (do 30″, jeśli sensor pozwala), ostrość z odległością, balans bieli i EV. |

- **RAW (DNG) i RAW+JPEG** z automatycznym wyborem sensora RAW, także gdy telefon udostępnia go jako osobny aparat fizyczny.

### Wideo
- 4K / 1080p / 720p, **HDR 10-bit (HLG)** i **stabilizacja**, jeśli urządzenie je obsługuje (automatyczny dobór do możliwości).
- **Zwolnione tempo** na głównym tylnym obiektywie 1×: wybór każdej wykrytej pary rozdzielczość + klatkaż (m.in. 120/240 kl/s, zależnie od telefonu).
- Pauza i wznowienie, latarka, dźwięk.
- **Płynne zbliżanie** – wolny, równy najazd podczas nagrywania.

### Dyktafon
- AAC/M4A: *Mowa* (96 kb/s mono), *Muzyka* (256 kb/s stereo), *Studio* (320 kb/s stereo) oraz *Bezstratny* WAV 16-bit 48 kHz.
- Nagrywanie w tle i przy zgaszonym ekranie, sterowanie z powiadomienia (pauza, znacznik, stop).
- Mikrofon wbudowany lub zewnętrzny (przewodowy, USB, Bluetooth LE), przetwarzanie: standard, surowy, głos, kierunkowy.
- Wzmocnienie do +24 dB z ogranicznikiem, redukcja szumów, automatyczna głośność, pomijanie ciszy, automatyczny stop.
- Mierniki szczytów L/P z ostrzeżeniem o przesterowaniu, znaczniki w trakcie nagrania.
- Biblioteka: dni, wyszukiwanie, zarys fali, przewijanie, ±10 s, prędkość 0,75–2×, znaczniki, zmiana nazwy, udostępnianie.

### Interfejs
- **Łukowe pokrętło trybów**, działające jak pierścień obiektywu, z zapadkami haptycznymi.
- **Płynny zoom**: szczypanie, skoki 0,5×/1×/2×/5×/10× i logarytmiczna linijka; interpolacja w przestrzeni logarytmicznej co klatkę ekranu.
- Dotknięcie = ostrość i pomiar, obok suwak ekspozycji; podwójne dotknięcie = zmiana obiektywu.
- Poziomica z czujnika grawitacji, histogram na żywo z ostrzeżeniem o przepaleniach, siatki (trójpodział, złoty podział, kwadrat, przekątne, celownik).
- Histogram, kompas i pasek parametrów można przytrzymać, przesunąć albo przeciągnąć do kosza; ustawienia w poziomie mają boczną nawigację.
- Ikony obracają się razem z telefonem, a klawisze głośności służą jako migawka albo zoom.

## Architektura

```
app/src/main/java/com/blacksingularities/registrer/
├── MainActivity.kt          # uprawnienia, klawisze, cykl życia
├── camera/                  # CameraEngine (CameraX + Camera2 Interop + Extensions), zapis MediaStore, histogram
├── computational/           # NightStacker, ExposureFusion, FrameMath – przetwarzanie pasami, wielowątkowo
├── audio/                   # AudioEngine (AudioRecord → AAC/WAV → MediaStore), usługa nagrywania w tle
├── data/                    # ustawienia (SharedPreferences)
└── ui/                      # CameraViewModel (MVVM) + Jetpack Compose: ekran i komponenty
```

- **Kotlin 2.0 · Jetpack Compose · Material 3 · CameraX 1.6**
- Potok obliczeniowy dekoduje klatki pasami (`BitmapRegionDecoder`), więc zużywa pamięć rzędu jednej klatki, a nie N klatek.
- Pliki: zdjęcia i wideo w `DCIM/OpenShot`, nagrania w `Recordings/OpenShot`.

## Budowanie lokalnie

Wymagany JDK 17 i Android SDK (API 35):

```bash
gradle assembleDebug
```

Wydania podpisuje CI kluczem z sekretów repozytorium (`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`, `SIGNING_KEY_ALIAS`).
