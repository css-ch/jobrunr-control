# Anleitung zur Erstellung eines neuen Batch-Jobs

Diese Anleitung basiert auf dem Beispiel in den Klassen `CalculationBatchJob`, `CalculationBatchJobRequest`,
`CalculationBatchJobPreparer`, `CalculationBatchJobPreparerRequest`, `CalculationItemProcessor` und
`CalculationItemProcessorRequest`. Diese Klassen demonstrieren, wie ein Batch-Job in JobRunr implementiert wird, bei dem
ein Hauptjob einen Preparer startet, der wiederum einzelne Item-Processor-Jobs enqueuet.

## Überblick

Ein Batch-Job in diesem Kontext besteht aus mehreren Komponenten:

- **BatchJobRequest**: Ein Record, das die Parameter für den Batch-Job definiert.
- **BatchJob**: Die Hauptklasse, die den Batch startet, indem sie einen Preparer-Job auslöst.
- **BatchJobPreparerRequest**: Ein Record für den Preparer-Job.
- **BatchJobPreparer**: Bereitet die Liste der zu verarbeitenden Items vor und enqueuet die Item-Processor-Jobs.
- **ItemProcessorRequest**: Ein Record für jedes einzelne Item.
- **ItemProcessor**: Verarbeitet ein einzelnes Item.

## Schritte zur Erstellung eines neuen Batch-Jobs

### 1. Definiere die Request-Klassen

Erstelle Records für die Requests. Diese implementieren `JobRequest` und geben die entsprechende Handler-Klasse zurück.

- **YourBatchJobRequest.java**: Definiert die Parameter für den gesamten Batch, z.B. `totalItems`, `batchSize`,
  `simulateErrors`.
  ```java
  public record YourBatchJobRequest(Integer totalItems, Integer batchSize, Boolean simulateErrors) implements JobRequest {
      @Override
      public Class<YourBatchJob> getJobRequestHandler() {
          return YourBatchJob.class;
      }
  }
  ```

- **YourBatchJobPreparerRequest.java**: Ähnlich, aber für den Preparer.
  ```java
  public record YourBatchJobPreparerRequest(Integer totalItems, Integer batchSize, Boolean simulateErrors) implements JobRequest {
      @Override
      public Class<YourBatchJobPreparer> getJobRequestHandler() {
          return YourBatchJobPreparer.class;
      }
  }
  ```

- **YourItemProcessorRequest.java**: Für jedes Item, z.B. `itemToCompute`, `simulateErrors`.
  ```java
  public record YourItemProcessorRequest(int itemToCompute, Boolean simulateErrors) implements JobRequest {
      @Override
      public Class<YourItemProcessor> getJobRequestHandler() {
          return YourItemProcessor.class;
      }
  }
  ```

### 2. Erstelle den BatchJob

Erstelle eine Klasse, die `ConfigurableJob<YourBatchJobRequest>` implementiert.

**Wichtig:** Die Verwendung von `ConfigurableJob` ist entscheidend, damit der Job im JobRunr-Dashboard (Board) erscheint
und über die Benutzeroberfläche konfiguriert und gestartet werden kann. Ohne diese Schnittstelle wird der Job nicht in
der UI angezeigt und kann nur programmatisch enqueuet werden.

- **YourBatchJob.java**:
  ```java
  @ApplicationScoped
  public class YourBatchJob implements ConfigurableJob<YourBatchJobRequest> {
      private static final Logger log = LoggerFactory.getLogger(YourBatchJob.class);
      
      @Override
      @Job(name = "YourBatchJob", retries = 2)
      public void run(YourBatchJobRequest request) throws Exception {
          YourBatchJobPreparerRequest preparerRequest = new YourBatchJobPreparerRequest(
                  request.totalItems(),
                  request.batchSize(),
                  request.simulateErrors()
          );
          BackgroundJobRequest.startBatch(preparerRequest);
      }
  }
  ```

### 3. Erstelle den BatchJobPreparer

Erstelle eine Klasse, die `JobRequestHandler<YourBatchJobPreparerRequest>` implementiert. Diese bereitet die Items vor
und enqueuet sie.

- **YourBatchJobPreparer.java**:
  ```java
  @ApplicationScoped
  public class YourBatchJobPreparer implements JobRequestHandler<YourBatchJobPreparerRequest> {
      @Job(name = "YourBatchJobPreparer", retries = 2)
      @Override
      public void run(YourBatchJobPreparerRequest request) throws Exception {
          jobContext().logger().info(String.format("Preparing batch job with totalItems: %d, batchSize: %d, simulateErrors: %b",
                  request.totalItems(), request.batchSize(), request.simulateErrors()));
          
          List<YourItemProcessorRequest> items = IntStream.rangeClosed(1, request.totalItems())
                  .mapToObj(x -> new YourItemProcessorRequest(x, request.simulateErrors()))
                  .toList();
          
          // Simuliere Vorbereitungsverzögerung
          try {
              Thread.sleep(5000);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
          }
          
          // Enqueue Hintergrundjobs für jedes Item
          BackgroundJobRequest.enqueue(items.stream());
      }
  }
  ```

### 4. Erstelle den ItemProcessor

Erstelle eine Klasse, die `JobRequestHandler<YourItemProcessorRequest>` implementiert. Diese verarbeitet jedes einzelne
Item.

- **YourItemProcessor.java**:
  ```java
  @ApplicationScoped
  public class YourItemProcessor implements JobRequestHandler<YourItemProcessorRequest> {
      private static final Random random = new Random();
      
      @Override
      @Transactional
      @Job(name = "Processing item: %0", retries = 0)
      public void run(YourItemProcessorRequest request) throws Exception {
          jobContext().logger().info(String.format("Processing item: %s", request));
          
          // Simuliere Verarbeitungsverzögerung
          try {
              Thread.sleep(5000);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
          }
          
          if (request.simulateErrors()) {
              if (random.nextDouble() < 0.01) {
                  jobContext().logger().error("Simulated error processing item: " + request.itemToCompute());
                  throw new Exception("Simulated error processing item: " + request.itemToCompute());
              }
          }
          
          jobContext().logger().info("Processing done.");
      }
  }
  ```

### 5. Anpassungen und Hinweise

- **Paket-Struktur**: Platziere die Klassen in einem geeigneten Paket, z.B. `ch.css.jobrunr.control.jobs.yourjob`.
- **Parameter**: Passe die Parameter in den Records an deine Anforderungen an. Zum Beispiel, wenn du keine Fehler
  simulieren möchtest, entferne `simulateErrors`.
- **Logik**: Ersetze die Platzhalter-Logik (z.B. `Thread.sleep`) durch deine tatsächliche Verarbeitungslogik.
- **Annotationen**: Verwende `@Job` für Namen und Retry-Zähler. Der Name kann Platzhalter wie `%0` für den ersten
  Parameter verwenden.
- **Transaktionen**: Verwende `@Transactional` im ItemProcessor, wenn Datenbankoperationen beteiligt sind.
- **Fehlerbehandlung**: Passe die Fehlerbehandlung an, z.B. Retry-Zähler.

Nach der Implementierung kannst du den Batch-Job starten, indem du
`BackgroundJobRequest.enqueue(new YourBatchJobRequest(...))` aufrufst.

Für weitere Informationen zu JobRunr, konsultiere die technische Spezifikation in `technical-spec.md` oder verwende
Context7 für Bibliotheksdetails.
