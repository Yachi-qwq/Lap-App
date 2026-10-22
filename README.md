# ⚡ Lap App – Elektrotechnik Quiz & LAP-Vorbereitung

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK%2030%2B-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design-3-757575.svg?style=for-the-badge&logo=materialdesign&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)

Eine moderne, elegante Android-App zur Vorbereitung auf die **Lehrabschlussprüfung (LAP)** im Bereich Elektrotechnik. Die App bietet strukturiertes Lernen nach Kapiteln, Echtzeit-Statistiken, ein dynamisches Bewertungssystem und ein dunkles Glassmorphism-Design.

---

## 📱 Screenshots

<div style="text-align: center;">

| Hauptmenü | Kapitelauswahl | Fragen lesen |
| :---: | :---: | :---: |
| <img src="screenshots/main_menu.png" width="230" alt="Hauptmenü"/> | <img src="screenshots/chapter_select.png" width="230" alt="Kapitelauswahl"/> | <img src="screenshots/read_mode.png" width="230" alt="Fragen lesen"/> |

| Lernmodus (Frage) | Lernmodus (Antwort & Bewertung) | Statistik & Fortschritt |
| :---: | :---: | :---: |
| <img src="screenshots/quiz_question.png" width="230" alt="Lernmodus Frage"/> | <img src="screenshots/quiz_answer.png" width="230" alt="Lernmodus Antwort"/> | <img src="screenshots/statistics.png" width="230" alt="Statistik"/> |

| Einstellungen | Cloud-Backup & Account |
| :---: | :---: |
| <img src="screenshots/settings.png" width="230" alt="Einstellungen"/> | <img src="screenshots/account_dialog.png" width="230" alt="Account Dialog"/> |

</div>

---

## ✨ Hauptfunktionen

### 📖 1. Fragen lesen & Durchsuchen
- Durchstöbere alle prüfungsrelevanten Fragen übersichtlich sortiert.
- Live-Suchfunktion nach **Frage-ID** oder **Textinhalten**.
- Farbliche Hervorhebung von gelernten und nicht-gelernten Fragen.

### 🎯 2. Üben & Lernmodus
- Gehe Fragen Schritt für Schritt kapitelweise durch (Kapitel A bis F).
- Inklusive Schaltplänen, Diagrammen und Bauteilabbildungen.
- Selbstbewertungssystem per Schalter:
  - 🔴 **Schlecht**: Frage erneut wiederholen.
  - 🟡 **Mittel**: Unsicherheiten vorhanden.
  - 🟢 **Gut**: Frage beherrscht.

### 🎲 3. LAP Prüfungsmodus
- Zufallsgenerierte Fragen aus allen Kapiteln oder gezielt aus einem ausgewählten Kapitel.
- Simulation unter realen Prüfungsbedingungen.

### 📊 4. Detaillierte Statistik & Drill-Down
- **Gesamtforschrittsanzeige** mit zirkulärem Fortschrittsbalken.
- Aufschlüsselung pro Kapitel (Offen, Schlecht, Mittel, Gut).
- **Drill-Down Modus**: Klicke direkt auf z. B. "Rot / Schlecht", um exakt die Fragen gezielt zu wiederholen.

### 🔔 5. Tägliche Erinnerung & Backup
- **Tägliche Push-Benachrichtigung** zur individuell einstellbaren Lernzeit.
- Automatischer Neustart der Benachrichtigungen nach einem Geräte-Neustart (`BOOT_COMPLETED`).
- Tägliche Hintergrund-Sicherung deines Fortschritts über den Android **WorkManager**.

---

## 🛠 Tech Stack & Architektur

| Komponente | Technologie / Bibliothek |
| :--- | :--- |
| **Programmiersprache** | Kotlin |
| **Min. SDK / Target SDK** | Android 11 (API 30) / Android 15 (API 36) |
| **UI Design System** | Material Design 3 (Glassmorphism, CardViews, Night Theme) |
| **Layouts** | ConstraintLayout, RecyclerView, MaterialCardView, Dynamic Themes |
| **Hintergrund-Tasks** | AndroidX `WorkManager`, `AlarmManager`, `BroadcastReceiver` |
| **Datenformat & Parser** | JSON (`fragen.json`), Google `Gson` |
| **Speicherung** | Android `SharedPreferences` |
| **Dependency Mgmt.** | Gradle Version Catalogs (`libs.versions.toml`) |

---

## 📁 Projektstruktur
