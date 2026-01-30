# UI Adapter Tests

Dieses Paket enthält Unit Tests für das `ch.css.jobrunr.control.adapter.ui` Package.

## Test-Abdeckung

### Utility-Klassen (Unit Tests ✅)

Die folgenden Utility-Klassen werden mit Unit Tests abgedeckt:

1. **InstantFormatterTest** - Tests für Datum/Zeit-Formatierung
2. **DashboardUrlUtilsTest** - Tests für Dashboard URL-Generierung
3. **JobSearchUtilsTest** - Tests für Job-Suchfunktionalität
4. **PaginationHelperTest** - Tests für Pagination-Logik
5. **ParameterValueFormatterTest** - Tests für Parameter-Formatierung (Swiss conventions)
6. **TemplateExtensionsTest** - Tests für Page-Range-Berechnung

**Gesamt: 41 Unit Tests** ✅

### Controller-Klassen (Integration Tests empfohlen)

Die Controller-Klassen (`DashboardController`, `ScheduledJobsController`, `JobExecutionsController`,
`TemplatesController`) sind **nicht** mit Unit Tests abgedeckt.

**Grund:**
Controller verwenden Quarkus Qute Templates mit `@CheckedTemplate`, die als `native` Methods implementiert sind. Diese
können nur im Quarkus-Kontext ausgeführt werden und sind nicht für einfache Unit Tests geeignet.

**Empfehlung:**
Für Controller sollten **Quarkus Integration Tests** mit `@QuarkusTest` verwendet werden. Diese Tests:

- Starten den vollständigen Quarkus-Kontext
- Verwenden echte Templates
- Testen die REST-Endpoints mit `@TestHTTPEndpoint` oder REST-Assured
- Können mit `@InjectMock` Dependencies mocken

Beispiel für einen Quarkus Integration Test:

```java

@QuarkusTest
@TestHTTPEndpoint(ScheduledJobsController.class)
class ScheduledJobsControllerIT {

    @InjectMock
    GetScheduledJobsUseCase getScheduledJobsUseCase;

    @Test
    void shouldReturnScheduledJobsPage() {
        when(getScheduledJobsUseCase.execute()).thenReturn(List.of());

        given()
                .when().get()
                .then()
                .statusCode(200)
                .contentType(MediaType.TEXT_HTML);
    }
}
```

## Clean Code Prinzipien

Alle Tests folgen Clean Code Best Practices:

✅ **Beschreibende Namen** - Jeder Test erklärt genau, was getestet wird  
✅ **Arrange-Act-Assert** - Klare 3-Phasen-Struktur  
✅ **Ein Konzept pro Test** - Fokus auf eine Funktionalität  
✅ **Sinnvolle Abdeckung** - Relevante Szenarien, keine Edge-Case-Explosion  
✅ **Wartbarkeit** - Einfach zu verstehen und zu erweitern
