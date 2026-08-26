package ch.css.jobrunr.control.jobs.recap;

import ch.css.jobrunr.control.annotations.DbBasedRecapAndMessages;
import ch.css.jobrunr.control.domain.details.JobMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobResultRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.String.format;

@DbBasedRecapAndMessages
@ApplicationScoped
public class RecapDemoWorker implements JobResultRequestHandler<RecapDemoWorkerRequest> {

    private static final Logger LOG = Logger.getLogger(RecapDemoWorker.class);
    private static final Random RANDOM = new Random();

    private final JobMessageService messageService;

    @Inject
    public RecapDemoWorker(JobMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public RecapDemoCounters runAndReturn(RecapDemoWorkerRequest jobRequest) {
        int policeNr = jobRequest.number();
        if (jobRequest.exception()) {
            messageService.info("Diese Meldung möchte ich aufgrund des Rollbacks nicht sehen.");
            messageService.infoTxNew("Diese Meldung möchte trotz des Rollbacks sehen.");
            if(ThreadLocalJobContext.getJobContext().currentRetry() < 2) {
                throw new RuntimeException("Druckerfehler: Papierstau im Drucker.");
            }
        }
        PolicenResult policenResult = randomValue(Arrays.asList(PolicenResult.values()));
        RecapDemoCounters recap;
        String message;
        switch (policenResult) {
            case FEHLER -> {
                message = format("[Police %s] Druck mit fachlichem Fehler abgebrochen: %s", policeNr, "Kein Korrespondenzempfänger für Versicherte Person '12550964' zum Tagesdatum gefunden.");
                ThreadLocalJobContext.getJobContext().logger().error(message);
                messageService.error(message);
                recap = RecapDemoCounters.builder()
                        .policenSelektiert(1)
                        .policenFailed(1)
                        .druckauftraegeVerarbeitet(1)
                        .build();
            }
            case GEDRUCKT -> {
                message = format("[Police %s] DruckAuftrag mit erfolgreich gedruckt.", policeNr);
                ThreadLocalJobContext.getJobContext().logger().info(message);
                messageService.info(message);
                recap = RecapDemoCounters.builder()
                        .policenSelektiert(1)
                        .policenRelevant(1)
                        .druckauftraegeVerarbeitet(1)
                        .druckauftraegeGedruckt(1)
                        .build();
            }
            case POLICENSPERRE -> {
                message = format("[Police %s] Police ist für den Druck gesperrt.", policeNr);
                ThreadLocalJobContext.getJobContext().logger().warn(message);
                messageService.warning(message);
                recap = RecapDemoCounters.builder()
                        .policenSelektiert(1)
                        .policenSperre(1)
                        .druckauftraegeVerarbeitet(1)
                        .build();
            }
            case AUSSELEKTIERT -> {
                message = format("[Police %s] DruckAuftrag mit wurde ausselektiert. Grund: PRAN Druck wird im Tagesgeschäft nicht verarbeitet.", policeNr);
                ThreadLocalJobContext.getJobContext().logger().warn(message);
                messageService.warning(message);
                recap = RecapDemoCounters.builder()
                        .policenSelektiert(1)
                        .policenHerausgefilter(1)
                        .druckauftraegeVerarbeitet(2)
                        .build();
            }
            case IRRELEVANT -> {
                message = format("[Police %s] Police ist nicht vorhanden oder bereits beendet.", policeNr);
                ThreadLocalJobContext.getJobContext().logger().warn(message);
                messageService.warning(message);
                recap = RecapDemoCounters.builder()
                        .policenSelektiert(1)
                        .policenAnnulliert(1)
                        .druckauftraegeVerarbeitet(1)
                        .build();
            }
            default -> throw new IllegalStateException("Unexpected value: " + policenResult);
        }
        LOG.info(message);
        return recap;
    }

    private static <T> T randomValue(List<T> values) {
        return values.get(RANDOM.nextInt(values.size()));
    }

    public enum PolicenResult {
        FEHLER,
        GEDRUCKT,
        POLICENSPERRE,
        AUSSELEKTIERT,
        IRRELEVANT
    }
}
