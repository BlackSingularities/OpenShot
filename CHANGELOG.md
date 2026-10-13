# Changelog

## 2.6.0

- Nowy tryb **Długi czas** (na karuzeli między Ręcznym a Nocą): seria klatek przez 1–15 s łączona w jedno zdjęcie, jak z filtrem ND lub statywem.
  - *Rozmycie ruchu* – średnia klatek: gładka woda, smugi chmur, znikający przechodnie.
  - *Smugi światła* – najjaśniejsze piksele: światła samochodów, gwiazdy, ogień.
- Czas wybierasz na pokrętle nad migawką, sposób łączenia – przyciskiem na górnym pasku; w ustawieniach trybu można wyłączyć wyrównywanie klatek (na statywie tło jest wtedy ostrzejsze).
- Seria robiona jest w ok. 12 MP dla szybkości (najwyżej 32 klatki); lampa w tym trybie działa tylko jako latarka.

## 2.5.0

- **Lokalizacja w zdjęciach i filmach** (domyślnie włączona): współrzędne GPS trafiają do EXIF zdjęć – także nocnych, HDR, portretów, ruchomych i przyciętych zoomem cyfrowym – oraz do metadanych filmów. Pozycja pochodzi z systemowej usługi lokalizacji (działa bez usług Google, np. na Huawei) i jest odświeżana tylko przy otwartej aplikacji. Przełącznik „Lokalizacja” w ustawieniach ogólnych; przy włączeniu aplikacja prosi o zgodę.
- Przełączniki w ustawieniach ogólnych w dwóch rzędach po trzy.

## 2.4.7

- Szeroki kąt na telefonach, które udostępniają ten sam sensor jako osobny aparat i jako część aparatu logicznego (np. Huawei Mate X7): aplikacja otwiera go jako osobny aparat zamiast przez aparat logiczny, czego producent nie obsługuje. W diagnostyce takie aparaty mają dopisek „też fizyczny”.

## 2.4.6

- Sekcja Aktualizacje jest na dole ustawień ogólnych, a podpis „Pomysł i wykonanie: redzikowski.com” z numerem wersji znajduje się w jej wnętrzu.

## 2.4.5

- Sekcja „Ten aparat” w ustawieniach ogólnych nazywa się teraz „Informacje o sprzęcie”.

## 2.4.4

- Rząd przełączników na górze ustawień ogólnych nie ma już nagłówka.

## 2.4.3

- Ustawienia ogólne są zwarte: proste przełączniki to siatka ikon (wibracje, dźwięk migawki, sygnał odliczania, parametry, kompas), a wybory – obrót ekranu, klawisze głośności, histogram i jego wielkość – mieszczą się w jednej linii z przełącznikiem segmentowym i bieżącą wartością.
- Automatyczne sprawdzanie i instalowanie aktualizacji to dwie pigułki pod wierszem wersji; informacje o aparacie są domyślnie zwinięte.
- Stopka „Pomysł i wykonanie: redzikowski.com” z numerem wersji jest widoczna na dole każdej zakładki ustawień.

## 2.4.2

- Ustawienia po obróceniu telefonu do poziomu mają nadal zakładki u góry i nie wchodzą pod pasek stanu – odstępy od pasków systemowych liczone są dla obróconego układu.
- Podpis przy wersji: „Pomysł i wykonanie · redzikowski.com” (poprawiona pisownia, adres otwiera się po dotknięciu).
- W ustawieniach ogólnych sekcja Aktualizacje jest pod sekcją Obsługa.

## 2.4.1

- Polskie nazwy zamiast zapożyczeń: tryby *Ręczny* (Pro), *Ruchome* (Live), *Zdjęcie* (Foto), *Zwolnione tempo* (Slow-mo) i *Dyktafon* (Audio); w ustawieniach pełne nazwy trybów.
- Także: *Płynne zbliżanie* (kinowy zoom), *Rozmycie tła* (bokeh), *Autoportret głównym aparatem* (selfie), *Ostrość i ekspozycja zablokowane* (AE/AF), *Szybkość (bez opóźnienia)* (ZSL), *Samoczynna głośność*, profile dźwięku *Studyjny* i przetwarzanie *Zwykłe*.

## 2.4.0

### Dyktafon
- Nowy silnik nagrywania: własne kodowanie AAC (M4A) oraz nowy format **Bezstratny** – WAV 16-bit 48 kHz stereo.
- Nagrywanie trwa **w tle i przy zgaszonym ekranie**; powiadomienie pokazuje czas i pozwala wstrzymać, wznowić, dodać znacznik i zakończyć nagranie. Po powrocie do aplikacji studio pokazuje trwające nagranie.
- **Mierniki szczytów L/P** w dBFS ze strefą przesterowania, wskaźnikiem szczytu i ostrzeżeniem „PRZESTER”.
- **Wybór mikrofonu**: auto, telefon albo zewnętrzny (przewodowy, USB, Bluetooth LE) – podłączenie urządzenia jest wykrywane na bieżąco, a nazwa aktywnego mikrofonu widnieje podczas nagrywania.
- **Cyfrowe wzmocnienie** do +24 dB z miękkim ogranicznikiem, systemowa **redukcja szumów** i **automatyczna głośność** (gdy telefon je udostępnia).
- **Pomijanie ciszy**: przerwy dłuższe niż 1,5 s nie trafiają do pliku (z zachowaniem początku kolejnej wypowiedzi).
- **Automatyczne zatrzymanie** po 15 min, 30 min, 1 h lub 2 h.
- **Znaczniki** w trakcie nagrywania – przyciskiem obok migawki albo z powiadomienia.
- Górny pasek trybu Audio ma szybkie przełączniki: mikrofon, wzmocnienie, redukcja szumów, pomijanie ciszy i automatyczny stop.
- Biblioteka nagrań: grupowanie dniami, wyszukiwanie, łączny czas i rozmiar, **zarys fali** z postępem i znacznikami, przewijanie dotknięciem lub przeciągnięciem, skoki ±10 s, prędkość 0,75–2×, skok do następnego znacznika, pauza i wznowienie, **zmiana nazwy**, udostępnianie i usuwanie.

### Tryby
- Nowa kolejność trybów na karuzeli: Pro · Noc · HDR · Portret · Live · **Foto · Wideo** · Slow-mo · Audio – Foto i Wideo są obok siebie, tryby fotograficzne po lewej, wideo i audio po prawej.
- Tryb „Pro Foto” nazywa się teraz „Pro”.
- Usunięto tryb Klip – krótkie chwile zapisuje tryb Live jako zdjęcie ruchome.
- Karuzela trybów ma większe ikony, a wybrany tryb jest wyraźnie powiększony.

### Poprawki
- Szeroki kąt: przycisk 0,6× i szczypanie korzystają z zoomu aparatu głównego, gdy ten sięga szerokiego kąta, zamiast przełączać się na osobny sensor fizyczny. Osobny obiektyw, który nie przesyła obrazu albo nie daje się włączyć, jest wykrywany i pomijany, a aplikacja wraca do aparatu głównego.
- Powiadomienie o aktualizacji jest na środku kadru i ponad nakładkami, więc przyciski „Pobierz” i „Zainstaluj” znów reagują.
- Histogram, kompas, pasek parametrów i nawigator zoomu przesuwa się od razu po ruchu palcem (albo po krótkim przytrzymaniu); kosz poprawnie wykrywa nakładkę także po zmianie układu ekranu.

### Ustawienia i wygląd
- Changelog w ustawieniach ma wyjustowany tekst i czytelniejszą interlinię.
- Przy numerze wersji widnieje podpis „Pomysł i wykonanie · redzikowski.com”.
- Aktywna zakładka ustawień automatycznie pozostaje widoczna.
- Ikona launchera jest odrobinę większa i nie ma narzuconego tła.

## 2.3.3

- Zmiana proporcji kadru nie przesuwa już nakładek pływających (histogram, pasek parametrów, kompas, nawigator zoomu) – chwilowe przebudowanie sterowania podczas ponownego uruchamiania aparatu nie zmienia ich obszaru.

## 2.3.2

- Ikona OpenShot ma teraz neutralną, czarno-białą paletę: srebrne metaliczne listki, grafitowe cienie i białe refleksy, nadal bez tła.

## 2.3.1

- Stabilizacja obrazu jest dostępna bezpośrednio na górnym pasku trybu Wideo, gdy obsługuje ją aktywny aparat.
- Przy zoomie większym niż 10× pojawia się ruchomy podgląd szerszego kadru z celownikiem pokazującym widoczny wycinek; jego pozycja jest zapamiętywana.
- Zoom powyżej 10× w trybach Foto i Pro przechodzi na przewidywalne kadrowanie programowe, dzięki czemu podgląd nawigacyjny pozostaje czytelny.

## 2.3.0

- Slow-motion wykrywa wszystkie obsługiwane połączenia rozdzielczości i klatkażu, działa wyłącznie na tylnym aparacie głównym 1× i udostępnia oba parametry na górnym pasku.
- Poprawiono wybór szerokiego kąta na podstawie rzeczywistej ogniskowej sensora oraz przejścia między 0,6× i 1×.
- Górne sterowanie wideo ustawia rozdzielczość i klatkaż, a pasek parametrów pokazuje aktywny format sesji.
- Histogram jest prostokątny. Histogram, kompas i pasek parametrów można przytrzymać, przesunąć lub przeciągnąć do kosza.
- Kompas jest niższy, a bieżący kierunek i liczba stopni leżą na jego osi centralnej.
- Regulatory EV, ostrości, rozmycia portretowego i liczby klatek nocnych są zwijane; Pro Foto ma osobne przełączniki EV i ostrości przy suwaku.
- Ustawienia w orientacji poziomej używają bocznej nawigacji. Lista wykrytych aparatów jest domyślnie zwinięta, a diagnostyka formatów została usunięta.
- Ujednolicono i zagęszczono rozwijane menu, w tym menu siatek.
- Uporządkowano karuzelę trybów i doprecyzowano nazwy Live, Klip oraz Pro Foto.
- Wspólne sekcje „Zdjęcie” i „Kadr” są zwinięte, ułożone pod opcjami charakterystycznymi dla trybu.
- Zmniejszono znak metalicznej migawki w ikonie, aby mieścił się w każdej masce launchera Androida.
