package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-based implementation of ParameterStoragePort.
 * Stores parameters in a separate database table using Jakarta Persistence.
 * Requires Hibernate ORM to be enabled.
 */
@ApplicationScoped
public class JpaParameterStorageAdapter implements ParameterStoragePort {

    private static final Logger log = Logger.getLogger(JpaParameterStorageAdapter.class);

    private final ObjectMapper objectMapper;
    private final Instance<EntityManager> entityManagerInstance;

    @Inject
    public JpaParameterStorageAdapter(ObjectMapper objectMapper, Instance<EntityManager> entityManagerInstance) {
        this.objectMapper = objectMapper;
        this.entityManagerInstance = entityManagerInstance;
    }

    private EntityManager getEntityManager() {
        if (!entityManagerInstance.isResolvable()) {
            throw new IllegalStateException(
                    "EntityManager is not available. Ensure quarkus.hibernate-orm.enabled=true " +
                            "when using EXTERNAL parameter storage strategy.");
        }
        return entityManagerInstance.get();
    }

    @Override
    @Transactional
    public void store(ParameterSet parameterSet) {
        try {
            ParameterSetEntity entity = new ParameterSetEntity();
            entity.id = parameterSet.id();
            entity.jobType = parameterSet.jobType();
            entity.parametersJson = objectMapper.writeValueAsString(parameterSet.parameters());
            entity.createdAt = parameterSet.createdAt();
            entity.lastAccessedAt = parameterSet.lastAccessedAt();

            getEntityManager().persist(entity);
            log.debugf("Stored parameter set: %s", entity.id);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize parameters", e);
        }
    }

    @Override
    public Optional<ParameterSet> findById(UUID id) {
        ParameterSetEntity entity = getEntityManager().find(ParameterSetEntity.class, id);
        if (entity == null) {
            return Optional.empty();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = objectMapper.readValue(
                    entity.parametersJson,
                    Map.class
            );

            return Optional.of(new ParameterSet(
                    entity.id,
                    entity.jobType,
                    parameters,
                    entity.createdAt,
                    entity.lastAccessedAt
            ));
        } catch (JsonProcessingException e) {
            log.errorf("Failed to deserialize parameters for set: %s", id, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        EntityManager em = getEntityManager();
        ParameterSetEntity entity = em.find(ParameterSetEntity.class, id);
        if (entity != null) {
            em.remove(entity);
            log.debugf("Deleted parameter set: %s", id);
        }
    }


    @Override
    @Transactional
    public void updateLastAccessed(UUID id) {
        EntityManager em = getEntityManager();
        ParameterSetEntity entity = em.find(ParameterSetEntity.class, id);
        if (entity != null) {
            entity.lastAccessedAt = Instant.now();
            em.merge(entity);
        }
    }
}
