# Quick Start: Maven Central Release einrichten

Folgen Sie diesen Schritten, um Releases auf Maven Central zu aktivieren.

## ⚡ Schnell-Setup (15-20 Minuten)

### Schritt 1: Maven Central Account (5 Min)

1. Registrieren Sie sich auf https://central.sonatype.com/
2. Verifizieren Sie Ihren Namespace `ch.css.jobrunr`:
    - Über GitHub Repository Ownership ODER
    - Über DNS TXT Record
3. Generieren Sie ein User Token:
    - Profil → View Account → Generate User Token
    - **Speichern Sie Username und Password!**

### Schritt 2: GPG Key erstellen (5 Min)

```bash
# Key generieren (wählen Sie RSA 4096, kein Ablauf)
gpg --full-generate-key

# Key ID anzeigen
gpg --list-secret-keys --keyid-format=long

# Beispiel Output:
# sec   rsa4096/ABCD1234EFGH5678 2026-02-02 [SC]
#       ^^^^^^^^^^^^^^^^^^^ Das ist Ihre KEY_ID

# Private Key exportieren
gpg --armor --export-secret-keys ABCD1234EFGH5678

# Public Key zu Keyserver hochladen
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EFGH5678
```

### Schritt 3: GitHub Secrets einrichten (5 Min)

Gehen Sie zu: **Repository → Settings → Secrets and variables → Actions**

Fügen Sie diese Secrets hinzu:

| Secret Name              | Wert                 | Quelle                                          |
|--------------------------|----------------------|-------------------------------------------------|
| `MAVEN_CENTRAL_USERNAME` | Token Username       | Maven Central User Token                        |
| `MAVEN_CENTRAL_PASSWORD` | Token Password       | Maven Central User Token                        |
| `GPG_PRIVATE_KEY`        | Kompletter Key Block | `gpg --armor --export-secret-keys`              |
| `GPG_PASSPHRASE`         | Ihr GPG Passwort     | Was Sie bei der Key-Erstellung eingegeben haben |
| `JOBRUNR_USERNAME`       | JobRunr Username     | Bereits vorhanden                               |
| `JOBRUNR_PASSWORD`       | JobRunr Password     | Bereits vorhanden                               |
| `JOBRUNR_PRO_LICENSE`    | License Key          | Bereits vorhanden                               |

### Schritt 4: Repository-URL anpassen (2 Min)

Falls nötig, aktualisieren Sie die GitHub-URL in `pom.xml`:

```xml

<scm>
    <connection>scm:git:git://github.com/IHR_USERNAME/jobrunr-control.git</connection>
    <developerConnection>scm:git:ssh://github.com:IHR_USERNAME/jobrunr-control.git</developerConnection>
    <url>https://github.com/IHR_USERNAME/jobrunr-control</url>
</scm>
```

### Schritt 5: Test-Release durchführen (3 Min)

1. Gehen Sie zu **Actions** → **Release to Maven Central**
2. Klicken Sie **"Run workflow"**
3. Eingabe: Version `0.1.0-test`
4. Klicken Sie **"Run workflow"**
5. Überwachen Sie die Workflow-Logs

## ✅ Checkliste vor dem ersten Release

- [ ] Maven Central Account verifiziert
- [ ] GPG Key erstellt und zu Keyserver hochgeladen
- [ ] Alle 7 GitHub Secrets konfiguriert
- [ ] Repository-URL korrekt (falls angepasst)
- [ ] Test-Release erfolgreich durchgelaufen
- [ ] Artefakt auf Maven Central sichtbar (nach 5-10 Min)

## 🚀 Ersten echten Release durchführen

Nach erfolgreichem Test:

1. **Actions** → **Release to Maven Central** → **Run workflow**
2. Version eingeben: `1.0.0`
3. Workflow starten

Der Workflow:

- Setzt Version auf `1.0.0`
- Baut das Projekt
- Führt Tests aus
- Signiert mit GPG
- Deployed zu Maven Central
- Erstellt Git-Tag `v1.0.0`
- Erstellt GitHub Release
- Setzt Version auf `1.0.1-SNAPSHOT`

## 📖 Vollständige Dokumentation

- **Detaillierte Anleitung:** `docs/RELEASE.md`
- **Secrets Setup:** `docs/GITHUB_SECRETS_SETUP.md`

## ❓ Häufige Probleme

### "401 Unauthorized" von Maven Central

→ User Token korrekt? Namespace verifiziert?

### "gpg: signing failed"

→ GPG_PASSPHRASE korrekt? Private Key vollständig kopiert?

### "Missing required metadata"

→ Sollte nicht passieren, alles ist bereits konfiguriert im POM

### Artefakt erscheint nicht auf Maven Central

→ Warten Sie 5-10 Minuten, prüfen Sie Workflow-Logs

## 🎯 Nächste Releases

Nach dem ersten erfolgreichen Release:

**Manuell über UI:**

1. Actions → Release to Maven Central → Run workflow
2. Version eingeben (z.B. `1.1.0`)
3. Fertig!

**Oder über GitHub Release:**

1. Neues Release erstellen
2. Tag `v1.1.0` erstellen
3. Release veröffentlichen
4. Workflow startet automatisch

## 💡 Tipps

- Verwenden Sie **Semantic Versioning** (MAJOR.MINOR.PATCH)
- Testen Sie neue Versionen zuerst mit `-test` Suffix
- Prüfen Sie Workflow-Logs bei Problemen
- Releases können **nicht überschrieben** werden auf Maven Central
- Halten Sie Ihre GPG Keys und Tokens sicher

## 🆘 Support

Bei Problemen:

1. Prüfen Sie Workflow-Logs in GitHub Actions
2. Konsultieren Sie `docs/RELEASE.md` → Troubleshooting
3. Überprüfen Sie Secret-Konfiguration in GitHub
