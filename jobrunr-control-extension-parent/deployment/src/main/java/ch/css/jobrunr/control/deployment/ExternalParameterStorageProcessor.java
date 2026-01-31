package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.infrastructure.persistence.ParameterSetEntity;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;

import java.util.Set;

public class ExternalParameterStorageProcessor {

    /**
     * Ensure the extension's runtime module is indexed by Jandex.
     * This is required for JPA entity discovery.
     */
    @BuildStep
    void indexExtensionRuntime(BuildProducer<IndexDependencyBuildItem> index) {
        index.produce(new IndexDependencyBuildItem("ch.css.jobrunr", "jobrunr-control-extension"));
    }

    /**
     * Register the JPA entity with Hibernate ORM.
     * This ensures the entity is properly discovered during build time.
     * Only registers if Hibernate ORM capability is present.
     */
    @BuildStep
    void registerEntity(Capabilities capabilities,
                        ParameterStorageBuildTimeConfig config,
                        BuildProducer<AdditionalJpaModelBuildItem> producer) {
        if (capabilities.isPresent(Capability.HIBERNATE_ORM)) {
            producer.produce(new AdditionalJpaModelBuildItem(
                    ParameterSetEntity.class.getName(),
                    Set.of(config.persistenceUnitName())
            ));
        }
    }

    /**
     * Register the entity for reflection (needed for native builds).
     * Only registers if Hibernate ORM capability is present.
     */
    @BuildStep
    void registerForReflection(Capabilities capabilities, BuildProducer<ReflectiveClassBuildItem> producer) {
        if (capabilities.isPresent(Capability.HIBERNATE_ORM)) {
            producer.produce(ReflectiveClassBuildItem.builder(
                    ParameterSetEntity.class.getName()
            ).methods().fields().build());
        }
    }
}
