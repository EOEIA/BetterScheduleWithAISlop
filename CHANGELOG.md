# Seznam změn / Changelog

## 2.0.25

- Text domácího úkolu lze nyní kopírovat: tlačítko kopírování u každého úkolu i v detailu hodiny, text v detailu hodiny jde označit.
- Opraveno zvýraznění aktuální hodiny, které zůstávalo viset na již skončené hodině.
- Opraveno pletení týdnů, když byla aplikace otevřená přes půlnoc nebo přes okamžik přepnutí na další týden.
- Nové motivy: Nord, Dracula, Solarized Light a Catppuccin Latte.
- Webová stránka a Nahlásit problém nyní míří na GitHub této fork verze místo na stránky původního autora.
- Společná záložka úkolů nyní jasně odděluje školní domácí úkoly od vlastních úkolů.
- Nová volitelná „Kompaktní karta příští hodiny“ (Nastavení → Rozvrh): dva řádky místo tří, s dnem v týdnu. Výchozí zůstává původní karta.
- Sloupce rozvrhu mají všechny stejnou šířku; sloupec s hodinami se změnšil na šířku svého obsahu a místo připadlo dnům.
- Obě karty mají stejný odstup od rozvrhu i od informačního řádku; dříve byla karta přilepená k tabulce.
- Odpočet se řídí tím, jak daleko hodina je („začíná za 2 dny“ místo „1d 21h 6m 46s“); sekundy až v poslední minutě, bez překreslování každou sekundu.
- Informační řádek nyní respektuje velikost písma z motivu, kterou dosud ignoroval.
- Tlačítko otočení tabulky ukazuje stav natočením šipek podle osy tabulky místo oranžové ikony.
- Odstraněna nastavení Otočená tabulka, Střídání řádků a Střídání sloupců. Otočení tabulky zůstává dostupné přes tlačítko v liště rozvrhu.
- Demo režim se nyní zapíná jen ve výběru účtu; přepnutím na skutečný účet se vypíná.

---

- Homework text can now be copied: a copy button on every homework card and in the lesson detail, and lesson-detail text is selectable.
- Fixed the current-lesson highlight getting stuck on a lesson that was already over.
- Fixed weeks getting mixed up when the app was left open across midnight or across the "switch to the next week" moment.
- New themes: Nord, Dracula, Solarized Light and Catppuccin Latte.
- Website and Report an issue now point at this fork's GitHub page instead of the original author's.
- The combined Homework tab now clearly separates school homework from your own tasks.
- New optional "Compact next lesson card" (Settings → Timetable): two lines instead of three, with the weekday shown. The original three-line card stays the default.
- Timetable columns are all the same width; the hour column shrank to the width of its own contents and the space went to the days.
- Both cards sit evenly clear of the timetable and the info line; the card used to be glued to the grid with a gap only underneath.
- Countdown now matches how far away the lesson is ("starts in 2 days" rather than "1d 21h 6m 46s"); seconds only in the final minute, and no more redrawing once a second when nothing changes that fast.
- Info line now honours the text size defined by the theme, which it had been ignoring.
- Transpose button shows its state by turning its arrows to match the table’s axis, instead of an orange icon.
- Removed the Transposed table, Alternating rows and Alternating columns settings. Transposing stays available via the toolbar toggle.
- Demo mode is now turned on only from the account picker and turned off by switching to a real account.

## 2.0.24

- Domácí úkoly nyní zobrazují skutečný text zadání místo kódů, načtený z Bakalářů a přiřazený ke skutečně zobrazenému týdnu.
- Upozornění na známky a úkoly se nyní kontrolují pravidelně na pozadí, ne jen při otevření dané obrazovky, s nastavitelným intervalem (Nastavení → Oznámení).
- Dialog s detailem hodiny má upravený vzhled, aby lépe vynikly důležité informace.

---

- Homework screen now shows real assignment text instead of raw codes, pulled fresh from Bakaláři and matched to the week actually being displayed.
- Grade and homework alerts now check periodically in the background instead of only when you open those screens, with a configurable interval (Settings → Notifications).
- Lesson-detail dialog restyled so the useful info stands out more.

## 2.0.23

- Rozvrh nyní na tabletech a velkých obrazovkách vyplní celé okno, místo aby zůstal v šířce telefonu. Na telefonu se nic nemění.

## 2.0.22

- Nové nastavení: Otočená tabulka — přepne rozvrh tak, aby dny byly ve sloupcích a hodiny v řádcích (Nastavení → Rozvrh).

---

- New setting: Transposed table — flip the timetable so days run across the top and time slots down the side (Settings → Timetable).

## 2.0.21

- Nastavení přehledně rozdělena do čtyř sekcí: Rozvrh, Vzhled, Oznámení, O aplikaci.
- Všechna oznámení na jednom místě.

---

- Settings reorganized into Timetable / Appearance / Notifications / About sections.
- All notification settings in one place.

## 2.0.20

- Novinka: Historie změn — Nastavení → Oznámení → Upozornění na změny → Historie změn zaznamenává každé upozornění.

---

- New: Change history — Settings → Notifications → Change alerts → Change history logs every timetable-change alert.

## 2.0.19

- Zvýraznění nových známek: odznak „NEW" na kartě předmětu a v detailu; zmizí po 3 s prohlížení.

---

- New grade highlighting: "NEW" badge on subject card and in detail view; clears after 3 s.

## 2.0.18

- Domácí úkoly: seskupeny podle data (sticky hlavička), řazení nejnovější/nejstarší/předmět, zobrazení času hodiny.

---

- Homework screen: grouped by date with sticky header, sort by newest/oldest/subject, lesson time shown.

## 2.0.17

- Prediktor: výběr váhy (1–10) zobrazí potřebnou známku pro přechod do lepšího pásma. Sticky průměrová lišta. Hypotetické známky v samostatné sekci.

---

- Grade predictor: weight chips (1–10), sticky avg bar, hypothetical marks in their own section.

## 2.0.16

- Přidána obrazovka diagnostiky oznámení (Nastavení → Školní novinky → Diagnostika oznámení).

---

- Added notification diagnostics screen (Settings → School updates → Notification diagnostics).

## 2.0.15

- Opravena matematika prediktoru známek (správné hranice stupňů průměru: ≤1,49, ≤2,49, ≤3,49, ≤4,49).
- Hypotetické úpravy se resetují při opuštění detailu předmětu.
- Přidáno zobrazení "Podle data" v záložce Známky.
- Nová obrazovka Domácí úkoly (dostupná ze Známek nebo hlavního panelu).
- Oznámení o nových známkách a úkolech (zapnout v Nastavení → Školní novinky).

---

- Fixed grade predictor math (correct grade band boundaries: ≤1.49, ≤2.49, ≤3.49, ≤4.49).
- Hypothetical grade edits now reset when leaving the subject detail.
- Added "By date" view in Grades tab.
- New Homework screen (reachable from Grades or main toolbar).
- Grade and homework notifications (enable in Settings → School updates).

## 2.0.14

- Novinka: Prediktor známek — klepnutím na předmět ve Známkách otevřeš detail, kde lze upravit nebo vyřadit existující známky a přidat hypotetické, aby ses podíval/a, jak by se změnil průměr.

---

- New: Grade predictor — tap any subject in Grades to open the detail view, edit or exclude grades, and add hypothetical grades to see how your average would change.

## 2.0.13

- Nová funkce: Upozornění na změny — volitelná oznámení při aktualizaci rozvrhu (odpadnutí, suplování, dny bez výuky). Zapnout v Nastavení → Upozornění na změny.

---

- New: Change alerts — optional notifications when the timetable is updated (cancellations, substitutions, no-school days). Enable in Settings → Change alerts.

## 2.0.11

- Klepnutím na buňku hodiny se zobrazí čas a živý odpočet v detailu hodiny.
- Klepnutím na kartu příští hodiny se otevře detail hodiny.
- Karta příští hodiny se skryje při prohlížení minulých nebo budoucích týdnů.
- Nové nastavení pro zobrazení nebo skrytí karty příští hodiny.

---

- Tapping a lesson cell now shows its time and a live countdown in the detail dialog.
- Tapping the next-lesson card opens the lesson detail dialog.
- Next-lesson card is hidden when browsing past or future weeks.
- New setting to show or hide the next-lesson card.

## 2.0.9

- Přidáno volitelné zvýraznění řádku aktuálního dne v celém rozvrhu.
- Detail hodiny nyní zobrazuje dostupné informace o domácím úkolu místo pouhého počtu.
- Demo režim obsahuje ukázkové známky.

---

- Added an optional setting to highlight today's row in the full timetable.
- Lesson details now show available homework information instead of only the count.
- Demo mode now includes sample grades.

## 2.0.8

- Debug buildy mají viditelný demo režim pro testování rozvrhu bez skutečného účtu.
- Demo data nyní obsahují ukázky domácích úkolů, přidaných, odebraných, suplovaných a přesunutých hodin i dne bez výuky.

---

- Debug builds now expose demo mode for testing the timetable without a real account.
- Demo data now includes homework, added, removed, substituted, room-changed lessons, and a no-school day.

## 2.0.7

- Na hlavní obrazovku rozvrhu přibyla kompaktní karta s aktuální nebo další hodinou, časem a odpočtem.
- Karta zobrazuje indikaci domácího úkolu nebo změny hodiny, pokud je relevantní.

---

- Added a compact current/next lesson card to the main timetable screen with time and live countdown.
- The card shows homework or lesson-change indicators when relevant.

## 2.0.3

- Rozvrh nyní při posouvání do stran ponechá viditelný kompaktní sloupec dnů. Lze změnit v nastavení.
- Vylepšeno interní rozpoznávání změn v rozvrhu.

---

- The timetable now keeps a compact day column visible while scrolling sideways. This can be changed in settings.
- Improved internal recognition of schedule changes.

## 2.0 (21. 1. 2025)

- Nyní se můžete přihlásit více účty najednou
- Vylepšeny motivy
- Aplikace nyní lépe funguje na nových verzích Androidu
- Všechny funkce jsou od teď zdarma. Děkuji všem, kteří v minulosti přispěli ❤️!

---

- Added support for multiple accounts
- Improved themes
- Updated for newer versions of Android
- All features are now free. A big thanks to everyone who donated so far ❤️!

## 1.8.11 (13. 1. 2024)

- Aktualizována Billing library

---

- Also updated Billing library

## 1.8.10 (11. 1. 2024)

- Aktualizováno pro Android 14

---

- Updated for Android 14

## 1.8.9 (11. 1. 2024)

- Opravena chyba, kvůli které aplikace nefungovala s nejnovější verzí serveru.

---

- Fixed a bug which caused the app to not work with the newest server version.

## 1.8.8 (2. 8. 2023)

- Oprava drobné změny v API

---

- Fix API change

## 1.8.5 (12. 8. 2022)

- Opraveno vysoké využití baterie pokud školní server soustavně neodpovídá

---

- Fix battery drain when school server does not respond permanently

## 1.8.4 (21. 9. 2021)

- Opravena chyba při importu motivu

---

- Fix theme import

## 1.8.3 (17. 6. 2021)

- Opraveno nekonečné načítání

---

- Fixed infinite loading

## 1.8.2 (21. 5. 2021)

- Další drobné opravy

---

- More minor fixes

## 1.8.1 (29. 4. 2021)

- Drobné opravy

---

- Minor fixes

## 1.8 (25. 4. 2021)

- Zjednodušeno přihlašování
- Velké změny "pod pokličkou" - aplikace je nyní rychlejší a spolehlivější.

---

- Improved login screen
- Big changes under the hood - app is now faster and more reliable.

## 1.7.3 (12. 9. 2020)

- Opravy chyb

---

- Bug fixes

## 1.7.2 (2. 9. 2020)

- Opravy chyb

---

- Bug fixes

## 1.7.1 (1. 9. 2020)

- Provedena bleskurychlá oprava pro nové API v3 (protože staré přestalo fungovat)
- Zbytečné prázdné hodiny na začátku a na konci dne jsou nyní automaticky odstraněny.

---

- Very quick hotfix for the new API v3 (as the old API stopped working)
- Redundant empty lessons at the start of day are now automatically removed.

## 1.7 (7. 6. 2020)

- Vylepšen vzhled
- Přidány motivy
- Widget nyní může mít jakoukoliv barvu a průhlednost
- V rozvrhu jsou zvýrazněny domácí úkoly
- Můžete podpořit vývojáře prostřednictvím plateb Google Play.

---

- Improved app look
- Added themes
- Widget now has customizable color and transparency
- Homework is now highlighted in the schedule.
- You can now support the developer through Google Play purchases

## 1.6.1 (9. 3. 2020)

- Opravy chyb

---

- Bug fixes

## 1.6 (1. 3. 2020)

- Konečně přidán widget s tmavým nebo světlým vzhledem
- Přidán seznam změn přímo v aplikaci

---

- Finally added widget with light or dark theme
- Added changelog right in the app


## 1.5.3 (25. 1. 2020)

- Použita jiná knihovna pro zobrazování open source licencí.

---

- Changed the library for displaying open source licenses.

## 1.5.2 (25. 1. 2020)

- Opravy chyb
- Povoleno nezabezpečené spojení (http), protože některé školy stále nepoužívají http**s**.

---

- Bug fixes
- Allowed for unencrypted connection (http), because some schools still don't use http**s**.

## 1.5.1 (1. 1. 2020)

- Opravy chyb
- Tolerance opravdu divných rozvrhů, ve kterých hodiny "vylézají" z nadepsaných bloků.

---

- Bug fixes
- Very weird schedules with lessons "outside of" captions are tolerated.

## 1.5 (31. 12. 2019)

Verze 1.5 přináší několik nových funkcí a také hromadu vylepšení a oprav, zvláště pro učitele a uživatele tabletů.

- Učitelům se v rozvrhu místo jejich vlastního jména zobrazuje název třídy.
- Do trvalého upozornění byly přidány tlačítka na rychlé listování mezi hodinami.
- Výrazně zlepšeno vykreslování rozvrhu, zvláště na tabletech či velmi malých obrazovkách. Už žádné překrývající se nápisy či bílé pruhy napravo od rozvrhu.
- Upraveno rozložení nápisů, aby byl rozvrh přehlednější.
- A několik dalších drobných vylepšení či oprav...

---

Version 1.5 brings several new features and plenty of tweaks and fixes, especially for teachers and tablet users.

- Teachers now see the class name instead of their own name in the schedule.
- The permanent notification has buttons to browse through the following or past lessons.
- Massively improved rendering of the schedule, mainly on tablets and small screens. No more white stripes on the right of the schedule or texts overlapping.
- Improved cell layout to make the schedule less cluttered.
- And many small tweaks and bug fixes...

## 1.4 (9. 12. 2019)

- Přidána funkce trvalého upozornění, díky které máte Vaši příští hodinu vždy na očích.
- Drobné zkrášlení nastavení na starších zařízeních.

---

- Added a persistent notification feature, thanks to which you don't have to open the any more!
- Slight improvement of the look of settings on older devices.

## 1.3.2 (5. 12. 2019)

- Opravena chyba, která způsobovala pád aplikace při zobrazování zvláštních rozvrhů.

---

- Fixed a bug which caused the app to crash when showing a weird schedule.

## 1.3.1 (3. 12. 2019)

- Snížena zátěž na školní server.
- Zvýšena tolerance (hodně) pomalých severů.

---

- Optimized load on school server.
- Increased request time-out (for very slow servers).

## 1.3

- Při spuštění skočí rozvrh na aktuální hodinu.
- Změněna ikona pro zobrazení aktuálního týdne.
- Vylepšena funkce "Napsat autorovi".

---

- Schedule now scrolls to current lesson on app start.
- Changed icon for showing current week.
- Improved feedback button.

## 1.2

- Přidáno automatické hlášení pádů, pro zlepšení stability aplikace.
- Rozvrh při otočení obrazovky už neskočí na začátek.
- Opraven popis aplikace na F-Droidu.
- Drobné opravy.

---

- Added automatic crash reporting system for better stability of the app.
- Schedule no longer jumps to beginning when screen is rotated.
- Fixed description on F-Droid.
- Minor fixes.
