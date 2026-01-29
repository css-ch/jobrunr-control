package ch.css.jobrunr.control.infrastructure.persistence;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for storing job parameter sets.
 * Uses standard Jakarta Persistence API.
 */
@Entity
@Table(name = "jobrunr_control_parameter_sets",
        indexes = {
                @Index(name = "idx_param_set_job_type", columnList = "job_type"),
                @Index(name = "idx_param_set_created", columnList = "created_at"),
                @Index(name = "idx_param_set_accessed", columnList = "last_accessed_at")
        })
@RegisterForReflection
public class ParameterSetEntity {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "job_type", nullable = false, length = 500)
    public String jobType;

    @Column(name = "parameters_json", nullable = false, columnDefinition = "TEXT")
    public String parametersJson;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    public Instant lastAccessedAt;

    @Version
    @Column(name = "version")
    public Long version;
}
