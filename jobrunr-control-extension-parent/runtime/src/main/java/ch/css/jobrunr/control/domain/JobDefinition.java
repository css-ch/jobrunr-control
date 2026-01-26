package ch.css.jobrunr.control.domain;

import java.util.List;

public record JobDefinition(String jobType,
                            boolean isBatchJob,
                            String jobRequestTypeName,
                            String handlerClassName,
                            List<JobParameter> parameters,
                            boolean isRecord) {

    public List<String> getParameterNames() {
        return parameters.stream()
                .map(JobParameter::name)
                .toList();
    }
}
