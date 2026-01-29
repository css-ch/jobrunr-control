package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.infrastructure.persistence.ParameterSetEntity;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

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
     */
    @BuildStep
    AdditionalJpaModelBuildItem registerEntity() {
        return new AdditionalJpaModelBuildItem(
                ParameterSetEntity.class.getName(),
                Set.of(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
        );
    }

    /**
     * Register the entity for reflection (needed for native builds).
     */
    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem.builder(
                ParameterSetEntity.class.getName()
        ).methods().fields().build();
    }
}
