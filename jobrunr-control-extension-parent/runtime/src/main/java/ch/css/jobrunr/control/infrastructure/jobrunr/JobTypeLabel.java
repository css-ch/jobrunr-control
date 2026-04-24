package ch.css.jobrunr.control.infrastructure.jobrunr;

/**
 * Central definition of the {@code jobtype:} label used to tag every scheduled job with its
 * originating handler. JobRunr caps every label at 45 characters (see
 * {@code AbstractJob.setLabels}), so the effective budget for the jobType is
 * {@link #MAX_JOBTYPE_LENGTH} characters.
 */
public final class JobTypeLabel {

    /**
     * Prefix prepended to the jobType when storing it as a JobRunr label.
     */
    public static final String PREFIX = "jobtype:";

    /**
     * JobRunr enforces a hard 45-character limit per label.
     */
    public static final int MAX_LABEL_LENGTH = 45;

    /**
     * Maximum allowed length for a jobType so {@code PREFIX + jobType} stays within the JobRunr
     * label limit.
     */
    public static final int MAX_JOBTYPE_LENGTH = MAX_LABEL_LENGTH - PREFIX.length();

    private JobTypeLabel() {
        // Utility class
    }

    /**
     * Builds the full {@code jobtype:} label value for a given jobType.
     */
    public static String stamp(String jobType) {
        return PREFIX + jobType;
    }

    /**
     * Extracts the jobType from a {@code jobtype:}-prefixed label, or {@code null} if the label
     * does not carry the prefix.
     */
    public static String extract(String label) {
        return label != null && label.startsWith(PREFIX) ? label.substring(PREFIX.length()) : null;
    }
}
