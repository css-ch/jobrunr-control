package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.DbBasedRecapAndMessages;
import ch.css.jobrunr.control.domain.details.JobMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobResultRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.String.format;

@DbBasedRecapAndMessages
@ApplicationScoped
public class ComplexParameterDemoChildJob implements JobResultRequestHandler<ComplexParameterDemoChildJobRequest> {

    private static final Logger LOG = Logger.getLogger(ComplexParameterDemoChildJob.class);
    private static final Random RANDOM = new Random();

    private final JobMessageService messageService;

    @Inject
    public ComplexParameterDemoChildJob(JobMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public ComplexParameterDemoJobRecap runAndReturn(ComplexParameterDemoChildJobRequest jobRequest) {
        int policeNr = jobRequest.number();
        if (jobRequest.exception()) {
            throw new RuntimeException("Druckerfehler: Papierstau im Drucker.");
        }
        ComplexParameterDemoChildJob.PolicenResult policenResult = randomValue(Arrays.asList(ComplexParameterDemoChildJob.PolicenResult.values()));
        ComplexParameterDemoJobRecap recap;
        String message;
        switch (policenResult) {
            case FEHLER -> {
                message = format("[Police %s] Druck mit fachlichem Fehler abgebrochen: %s", policeNr, "Kein Korrespondenzempfänger für Versicherte Person '12550964' zum Tagesdatum gefunden.");
                ThreadLocalJobContext.getJobContext().logger().error(message);
                messageService.error(message);
                recap = ComplexParameterDemoJobRecap.builder()
                        .policenSelektiert(1)
                        .policenFailed(1)
                        .druckauftraegeVerarbeitet(1)
                        .build();
            }
            case GEDRUCKT -> {
                message = format("[Police %s] DruckAuftrag mit erfolgreich gedruckt.", policeNr);
                ThreadLocalJobContext.getJobContext().logger().info(message);
                messageService.info(message);
                recap = ComplexParameterDemoJobRecap.builder()
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
                recap = ComplexParameterDemoJobRecap.builder()
                        .policenSelektiert(1)
                        .policenSperre(1)
                        .druckauftraegeVerarbeitet(1)
                        .build();
            }
            case AUSSELEKTIERT -> {
                message = format("[Police %s] DruckAuftrag mit wurde ausselektiert. Grund: PRAN Druck wird im Tagesgeschäft nicht verarbeitet.", policeNr);
                ThreadLocalJobContext.getJobContext().logger().warn(message);
                messageService.warning(message);
                recap = ComplexParameterDemoJobRecap.builder()
                        .policenSelektiert(1)
                        .policenHerausgefilter(1)
                        .druckauftraegeVerarbeitet(2)
                        .build();
            }
            case IRRELEVANT -> {
                message = format("[Police %s] Police ist nicht vorhanden oder bereits beendet.", policeNr);
                ThreadLocalJobContext.getJobContext().logger().warn(message);
                messageService.warning(message);
                recap = ComplexParameterDemoJobRecap.builder()
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
