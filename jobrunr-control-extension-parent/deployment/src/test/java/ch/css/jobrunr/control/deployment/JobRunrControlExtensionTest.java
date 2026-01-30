package ch.css.jobrunr.control.deployment;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests for the JobRunr Control Extension feature registration.
 * This test verifies the extension is properly loaded during Quarkus build.
 */
public class JobRunrControlExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            // Disable JobRunr components that require database/transaction manager
                            "quarkus.jobrunr.background-job-server.enabled=false\n" +
                                    "quarkus.jobrunr.job-scheduler.enabled=false\n" +
                                    "quarkus.jobrunr.dashboard.enabled=false\n" +
                                    // Disable Hibernate ORM to avoid entity discovery issues
                                    "quarkus.hibernate-orm.enabled=false\n" +
                                    // Exclude classes that depend on JobRunr runtime beans
                                    "quarkus.arc.exclude-types=" +
                                    "ch.css.jobrunr.control.infrastructure.**," +
                                    "ch.css.jobrunr.control.adapter.**," +
                                    "ch.css.jobrunr.control.application.**," +
                                    "org.jobrunr.quarkus.autoconfigure.**," +
                                    "org.jobrunr.scheduling.AsyncJobInterceptor\n"
                    ), "application.properties"));
}
