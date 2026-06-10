package ch.css.jobrunr.control.infrastructure.details;

import java.util.Map;

/**
 * Build-time generated extractor for recap values.
 */
public interface RecapValueExtractor {

    /**
     * @return fully qualified class name of the recap record this extractor supports
     */
    String recapClassName();

    /**
     * Extracts recap values from the provided recap object.
     *
     * @param recap recap object instance
     * @return map of recap field names to numeric values
     */
    Map<String, Long> extract(Object recap);
}
