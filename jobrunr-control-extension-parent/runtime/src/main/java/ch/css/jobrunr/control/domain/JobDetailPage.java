package ch.css.jobrunr.control.domain;

/**
 * The JobDetailPage defines the Detail-Result-Page for a Job
 *
 * @param recapParameterClass               Fully qualified name of the class used for parameter recap in the history-tab
 * @param messageProviderKey                Optional registry key for a custom message provider
 * @param recapProviderKey                  Optional registry key for a custom recap provider
 * @param showRecapParameterWithZeroValue   Is this parameter set, so the recap parameter is also shown when it has a zero value
 * @param showEmptyParameters               Whether to show parameters with empty values in the recap
 ***/
public record JobDetailPage(
        String recapParameterClass,
        String messageProviderKey,
        String recapProviderKey,
        boolean showRecapParameterWithZeroValue,
        boolean showEmptyParameters
) {}
