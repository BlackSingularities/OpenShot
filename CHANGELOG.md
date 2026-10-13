# Changelog

## 2.22.0

- **Górne przyciski**: dotknięcie pokazuje wszystkie dostępne opcje (lampa, samowyzwalacz, proporcje, format, rozdzielczość, klatkaż, braketowanie, ustawienia dyktafonu…) – wybierasz dotknięciem, dotknięcie obok zamyka listę. Przytrzymanie też rozwija listę: przesuń palec i puść, by wybrać jednym ruchem; puszczenie bez wyboru zostawia listę otwartą.
- **Histogram w trybie Wideo** (i w trybie ręcznym wideo oraz przy zdjęciach ruchomych) – włączany w ustawieniach trybu → Kadr.

## 2.21.1

- Naprawiono pokrętło trybów: po powrocie do trybu, w którym aplikacja startowała (np. Zdjęcie → Wideo), pokrętło pokazywało nowy tryb, ale aparat zostawał w poprzednim – w „Wideo” robił zdjęcia. Pokrętło zawsze wraca też na rzeczywiście aktywny tryb, gdy przełączenie jest niemożliwe.

## 2.21.0

- **Przyciąganie nakładek do zdjęcia**: parametry, kompas, histogram, wskaźnik dźwięku i nawigator zoomu podczas przeciągania „przyklejają się” do środka kadru (w poziomie i pionie) oraz do górnej, dolnej i bocznych krawędzi zdjęcia – z wibracją i linią pomocniczą.
- Nakładki można przeciągać bliżej dolnego sterowania aparatu (mniejszy margines).
- **Pozycje nakładek zapamiętywane osobno dla każdego trybu i każdego ekranu** – na składanym telefonie inne na ekranie zewnętrznym, inne na rozłożonym. Dotychczasowe położenia są punktem wyjścia.

## 2.20.0

- **Interfejs pod ekrany AMOLED**: tła w czystej czerni (zgaszone piksele) – ustawienia, dyktafon, panele, przyciski i plakietki. Zamiast szarych wypełnień elementy mają cienką obwódkę, a wybrane pozostają białe; dyktafon nie ma już fioletowego gradientu.

## 2.19.1

- Naprawiono budowanie aplikacji (konflikt nazw w obsłudze wskaźnika dźwięku w wideo) – wydania 2.9–2.19 zawierają się w tej wersji.

## 2.19.0

- **Domyślny aparat**: nowa opcja w Ustawieniach → Ogólne → Sterowanie pokazuje, który aparat jest domyślny, i pomaga to zmienić – gdy żaden, otwiera systemowy wybór (wybierz OpenShot → Zawsze), gdy inna aplikacja – jej ustawienia, by wyczyścić „Otwieraj domyślnie”.
- **Aparat dla innych aplikacji**: OpenShot obsługuje zlecenia zdjęcia i filmu (np. z komunikatora czy formularza w przeglądarce) – po zrobieniu zdjęcia lub nagraniu filmu wynik wraca do aplikacji, która o niego prosiła.
- **Start z ekranu blokady**: aparat otwierany przy zablokowanym telefonie (np. podwójnym naciśnięciem przycisku zasilania) pokazuje się nad ekranem blokady.

## 2.18.0

- **Płaski profil (LOG)** w wideo trybu ręcznego: logarytmiczna krzywa tonalna podnosi cienie i łagodnie kompresuje światła, a wyostrzanie jest wyłączone – materiał do późniejszej korekcji kolorów. Przełącznik „LOG” na górnym pasku i w ustawieniach trybu ręcznego; pojawia się tylko, gdy sterownik aparatu pozwala na własną krzywą (nie łączy się z HDR).

## 2.17.0

- **Braketowanie w trybie ręcznym** (zdjęcia): seria 3, 5 lub 7 zdjęć z krokiem ±0,7, ±1 albo ±2 EV, każde zapisane osobno – także w RAW – do późniejszej obróbki HDR. Przy ręcznej ekspozycji zmieniany jest czas naświetlania, w trybie automatycznym kompensacja EV. Włączasz je przyciskiem na górnym pasku albo w ustawieniach trybu ręcznego.

## 2.16.0

- **Szybszy start aparatu**: wyniki sprawdzania obiektywów, formatów i profili zwolnionego tempa są zapamiętywane na telefonie, więc przy kolejnym uruchomieniu kamera startuje od razu. Świeże sprawdzenie odbywa się w tle po starcie; aktualizacja systemu lub aplikacji unieważnia zapamiętane dane.

## 2.15.0

- **Pełna angielska wersja językowa** / *Full English localization*: cały interfejs, komunikaty, powiadomienia, kafelek, skróty i opisy ustawień.
- Język wybiera się automatycznie według systemu (także według języka ustawionego dla samej aplikacji w Androidzie 13+) albo ręcznie w Ustawieniach → Ogólne → Język: Auto / Polski / English. Zmiana działa od razu, bez ponownego uruchamiania.
- Liczby, daty i jednostki formatowane według języka (przecinek lub kropka dziesiętna, kl/s lub fps).

## 2.14.0

- **Skróty z ikony aplikacji** (przytrzymaj ikonę OpenShot): *Nagraj dźwięk* – od razu zaczyna nagrywać dyktafonem, *Wideo*, *Autoportret* – przedni aparat w trybie zdjęć.
- **Kafelek „Dyktafon” w szybkich ustawieniach**: dotknięcie otwiera aplikację i startuje nagrywanie, a podczas nagrywania kończy je bez otwierania aplikacji. Kafelek pokazuje, czy nagranie trwa.

## 2.13.0

- **Nowy układ ustawień ogólnych**, pogrupowany według tego, czego się szuka: szybkie przełączniki (wibracje, migawka, odliczanie, lokalizacja), „Na ekranie” – wszystkie nakładki jako kafelki (parametry, kompas, obrys ostrości, zebra, dźwięk) z histogramem, „Sterowanie” (obrót, klawisze głośności, podgląd na drugim ekranie), zwinięte „Poziomica” i „Informacje o sprzęcie”, na dole „Aplikacja” z aktualizacjami i autorem. Próg zebry pojawia się dopiero po jej włączeniu.
- **Płynny zoom wraca na szeroki kąt**: szczypanie poniżej 1× przełącza na obiektyw szerokokątny (wcześniej małe kroki szczypania nie przekraczały progu), a szeroki kąt startuje tuż pod 1×, więc przejście jest płynne w obie strony.
- **Tryb ręczny**: wyraźny przełącznik ZDJĘCIE | WIDEO nad panelem ręcznym, przy migawce; w wariancie wideo migawka jest czerwona i nagrywa film.

## 2.12.0

- **Podgląd dla fotografowanego na drugim ekranie**: przycisk na górnym pasku (pojawia się, gdy telefon ma dostępny drugi ekran) pokazuje obraz z aparatu na tylnym ekranie składanego telefonu albo na ekranie podłączonym lub przesyłanym. Fotografowany widzi kadr, a Ty fotografujesz dalej na głównym ekranie.
- Korzysta z systemowego trybu „Dual Screen” (Jetpack WindowArea), a gdy go brak – z dowolnego dodatkowego ekranu widocznego w systemie. Domyślnie obraz jest lustrzany (przełącznik „Lustro na 2. ekranie” w Pomocach kadru).
- „Informacje o sprzęcie” pokazują listę wykrytych ekranów i dostępność tylnego ekranu – pomocne, gdy przycisk się nie pojawia.

## 2.11.0

- Nowy tryb **Poklatkowy** (między Wideo a Zwolnionym tempem): zdjęcia co kilka sekund składane na bieżąco w film MP4 **1080p, 4K albo 8K** (HEVC, a gdy go brak – H.264). Na dysku nie zostają setki zdjęć, a film jest gotowy zaraz po zatrzymaniu.
- **Przyspieszenie z podpowiedzią sceny** na pokrętle nad migawką: Tłum ×15, Ulica ×30, Ruch ×60, Chmury ×120, Zachód ×300, Gwiazdy ×600, Budowa ×1800; odstęp między zdjęciami wynika z przyspieszenia i klatkażu filmu (24/30/60 kl/s), a podpowiedź pokazuje, ile sekund filmu daje minuta nagrania.
- Rozdzielczość i klatkaż na górnym pasku, automatyczne zakończenie po 5 min – 2 h. Aplikacja sama sprawdza, czy aparat i koder pozwalają na 8K – jeśli nie, film powstaje w najwyższej możliwej rozdzielczości.
- **Wyrównywanie jasności** tłumi migotanie między klatkami; opcjonalna **blokada ekspozycji** i balansu bieli.
- W trakcie nagrywania plakietka pokazuje liczbę klatek, długość powstającego filmu i czas; wyjście z aplikacji kończy film z dotychczasowymi klatkami.

## 2.10.0

- **Tryb ręczny dla wideo**: tryb Ręczny przełącza się między zdjęciami a wideo przyciskiem na górnym pasku (albo w ustawieniach trybu). W wideo działają ręczne ISO, czas naświetlania, ostrość i balans bieli, a do tego rozdzielczość, klatkaż, HDR, stabilizacja i dźwięk jak w trybie Wideo – z osobnymi ustawieniami.
- Czas naświetlania w wideo jest ograniczany do czasu klatki, by nagranie zachowało płynność.

## 2.9.0

- **Wskaźnik poziomu dźwięku w wideo** (włączany w ustawieniach ogólnych → Pomoce kadru): pływa po kadrze jak kompas i histogram – przeciągnij go w dowolne miejsce albo upuść na koszu.
- Pokazuje szczyt w dBFS z czerwoną strefą od −6 dB i opadającym znacznikiem szczytu. Przed nagraniem podsłuchuje mikrofon, którego użyje film; w trakcie nagrania odczytuje poziom prosto z nagrywanej ścieżki – działa tak samo z mikrofonem wbudowanym, przewodowym, USB i Bluetooth.
- Podpis pod wskaźnikiem: nazwa mikrofonu albo ostrzeżenie (wyciszony, mikrofon zajęty przez inną aplikację, błąd dźwięku).

## 2.8.0

- **Obrys ostrości** (focus peaking): najostrzejsze krawędzie kadru podświetlone na czerwono – ułatwia ręczne ustawianie ostrości.
- **Zebra**: ukośne paski na prześwietlonych obszarach, próg 85/90/95/98% jasności.
- Obie pomoce włącza się w ustawieniach ogólnych (sekcja „Pomoce kadru”) i działają we wszystkich trybach z podglądem: zdjęcia, wideo, zdjęcia ruchome, noc, HDR, długi czas i tryb ręczny (poza efektami sprzętowymi producenta). Maska uwzględnia obrót, przedni aparat i zoom cyfrowy.

## 2.7.0

- **Rozbudowana poziomica** z czterema wyglądami do wyboru w ustawieniach ogólnych:
  - *Horyzont* – linia z przerwą, stałe znaczniki ekranu i punkt pochylenia,
  - *Lotniczy* – sztuczny horyzont z drabinką pochylenia co 5° i łukiem przechyłu ze wskaźnikiem,
  - *Libelle* – dwie poziomice rurkowe: przechył na dole kadru, pochylenie z boku,
  - *Krzyż* – obracany celownik względem stałego koła.
- Odczyt przechyłu i pochylenia w stopniach; zdjęcie z góry zawsze pokazuje libellę okrągłą z odczytem obu osi.
- Dokładność „prosto” do wyboru (±0,5°, ±1°, ±2°), opcjonalne ukrycie pochylenia i przygaszanie poziomicy po wypoziomowaniu.
- **Kalibracja**: połóż telefon na płaskim stole i dotknij „Kalibruj” – zmierzone przesunięcie czujnika jest zapisywane i odejmowane od odczytów („Zeruj” je usuwa).

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
