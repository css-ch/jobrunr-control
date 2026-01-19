# Funktionale Spezifikation: JobRunr Pro Scheduler Extension

## 1. Überblick

Diese Komponente erweitert das bestehende JobRunr Pro Dashboard um eine dedizierte Benutzeroberfläche zur Planung,
Parametrierung und Überwachung von Jobs. Sie agiert als Client für die JobRunr API (Single Source of Truth) ohne eigene
Datenhaltung.

## 2. Datenmodelle & Typisierung

### 2.1 Job-Definition

Eine Job-Definition ist die Vorlage für einen ausführbaren Job.

* **Quelle:** JobRunr API.
* **Eigenschaften:**
    * **Job-Typ:** Der einfache Klassenname (SimpleClassName) der Job-Klasse (z.B. `ReportGeneratorJob`). Wird verwendet
      zur Kategorisierung und Anzeige.
    * **Vollständiger Klassenname:** Paket/Klassen-Pfad (z.B. `ch.css.jobs.ReportGeneratorJob`)
    * **Parameter-Definitionen:** Liste der erforderlichen Parameter-Definitionen.

### 2.2 Geplanter Job (Scheduled Job Instance)

Ein geplanter Job ist eine konkrete Instanz einer Job-Definition mit spezifischen Parameterwerten.

* **Quelle:** JobRunr API / Storage Provider.
* **Eigenschaften:**
    * **Job-ID:** Eindeutiger Identifier (UUID)
    * **Job-Name:** Frei wählbarer Name für diese spezifische Job-Instanz (z.B. `Monatsbericht Januar 2026`). Wird als
      Parameter in der JobRunr Job-Konfiguration gespeichert.
    * **Job-Typ:** SimpleClassName der zugrunde liegenden Job-Definition (z.B. `ReportGeneratorJob`)
    * **Geplanter Zeitpunkt:** Instant der geplanten Ausführung
    * **Parameter:** Map mit konfigurierten Werten
    * **Externer Trigger:** Boolean-Flag (abgeleitet vom Datum 31.12.2999)

### 2.2 Parameter-Typen

Jeder Job-Parameter muss einem der folgenden Typen zugeordnet sein:

| Datentyp     | Beschreibung      | UI-Repräsentation (Erwartet)           |
|--------------|-------------------|----------------------------------------|
| **String**   | Freitext          | Textfeld (Input Text)                  |
| **Integer**  | Ganzzahl          | Zahlenfeld (Number Input)              |
| **Boolean**  | Wahrheitswert     | Checkbox oder Toggle-Switch            |
| **Date**     | Datum (ohne Zeit) | Datumsauswahl (Date Picker)            |
| **DateTime** | Datum und Uhrzeit | Datum- & Zeitauswahl (DateTime Picker) |

### 2.3 Job-Status (Ausführungs-Historie)

Der Status eines ausgeführten Jobs kann folgende Werte annehmen:

* `Enqueued` (Ausstehend)
* `Processing` (In Bearbeitung)
* `Succeeded` (Abgeschlossen)
* `Failed` (Fehlgeschlagen)

---

## 3. Benutzeroberfläche (Views)

Das UI ist rein funktional für interne Benutzer auszulegen (Clean Design, keine Animationen).

### 3.1 View A: Geplante Jobs (Scheduler-Übersicht)

Diese Ansicht listet alle Jobs auf, die für die Zukunft geplant sind oder als "Extern Triggerbar" markiert wurden.

**Tabellen-Spalten:**

1. **Job-ID**
2. **Job-Name** (Frei wählbarer Name dieser Job-Instanz, z.B. "Monatsbericht Januar 2026")
3. **Job-Typ** (SimpleClassName der Job-Klasse, z.B. "ReportGeneratorJob")
4. **Geplanter Zeitpunkt** (Formatierung: Lokalzeit, oder "Externer Trigger" bei Datum 31.12.2999)
5. **Parameter** (Zusammenfassung der konfigurierten Werte)
6. **Aktionen** (Button-Gruppe: Bearbeiten, Löschen, Jetzt ausführen)

**Filter- & Such-Funktionen:**

* Suche über Freitext (muss Job-Name und Parameter-Werte durchsuchen).
* Filter nach Job-Typ.

**Interaktionen:**

* **Erstellen:** Öffnet Modal "Job Planen".
* **Bearbeiten:** Öffnet Modal "Job Planen" mit vorausgefüllten Werten des gewählten Jobs.
* **Löschen:** Entfernt den geplanten Job (nach Bestätigung).
* **Jetzt ausführen:** Führt eine Kopie des geplanten Jobs sofort aus (Trigger).

### 3.2 View B: Ausführungshistorie (Execution Monitor)

Diese Ansicht zeigt aktive und vergangene Jobs.

**Tabellen-Spalten:**

1. **Job-ID**
2. **Job-Name** (Frei wählbarer Name dieser Job-Instanz)
3. **Job-Typ** (SimpleClassName der Job-Klasse)
4. **Status** (siehe 2.3)
5. **Batch-Fortschritt** (Nur sichtbar, wenn Job = Batch Job):
    * Anzeigeformat: `Total` | `Erfolgreich` | `Fehlgeschlagen`
6. **Zeitstempel** (Startzeit / Endzeit)
7. **Deep-Link:** Button/Icon zum Öffnen der Detailansicht im originalen JobRunr Pro Dashboard.

**Besonderes Verhalten:**

* **Asynchrones Laden:** Da das Ermitteln von Subjobs für Batch-Prozesse rechenintensiv ist, darf die Ladezeit dieser
  Tabelle das UI nicht blockieren (Ladeindikator/Skeleton-Loader erforderlich).

### 3.3 Modal: Job Planen / Bearbeiten

Ein Dialogfenster zur Konfiguration eines Jobs.

**Eingabefelder:**

1. **Job-Typ-Auswahl:** Dropdown aller verfügbaren Job-Definitionen (von JobRunr API). Zeigt den SimpleClassName der
   Job-Klasse an.
2. **Job-Name:** Freitextfeld für einen benutzerdefinierten Namen dieser Job-Instanz (z.B. "Monatsbericht Januar 2026").
   Wird als Parameter in der JobRunr Job-Konfiguration gespeichert.
3. **Parameter-Formular:**
    * Wird *dynamisch* generiert basierend auf der Auswahl in (1).
    * Verwendet die Mappings aus Tabelle 2.2.
4. **Zeitplanung (Scheduling):**
    * Option A: **Einmalig** (Datum/Zeit wählen).
    * Option B: **Periodisch** (CRON-Ausdruck oder Intervall-Wähler).
    * Option C: **Externer Trigger** (Setzt Datum fix auf `31.12.2999` im Backend).

---

## 4. Geschäftslogik & Regeln

### 4.1 Validierung

* Die Eingabewerte im Modal müssen dem Datentyp des Parameters entsprechen (z.B. kein Text in Integer-Feld).
* Pflichtfelder müssen vor dem Speichern geprüft werden.

### 4.2 Externer Trigger (Konvention)

* Es gibt kein natives Flag für "Externer Trigger".
* **Logik:** Wenn ein Job das Ausführungsdatum `31.12.2999` besitzt, muss das UI diesen als "Wartet auf externen
  Trigger" anzeigen und nicht als kalendarisches Datum.

### 4.3 Doppel-Implementierung vermeiden

* Es werden keine Detail-Ansichten (Logs, Stacktraces) in dieser Komponente gebaut.
* Für Details muss immer per Deep-Link auf das bestehende JobRunr Dashboard verwiesen werden.

### 4.4 Persistenz

* **Read:** Alle Daten (Definitionen, Status, Pläne) werden live oder gecacht von der JobRunr API gelesen.
* **Write:** Das Speichern eines geplanten Jobs erfolgt durch einen API-Call an JobRunr. Es wird keine lokale Datenbank
  für diese Komponente angelegt.

