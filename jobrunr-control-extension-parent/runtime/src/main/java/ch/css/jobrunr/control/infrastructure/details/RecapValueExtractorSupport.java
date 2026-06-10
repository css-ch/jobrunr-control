package ch.css.jobrunr.control.infrastructure.details;

/**
 * Helper methods used by build-time generated recap extractors.
 */
public final class RecapValueExtractorSupport {

    private RecapValueExtractorSupport() {
    }

    public static long toLong(int value) {
        return value;
    }

    public static long toLong(long value) {
        return value;
    }

    public static long toLong(Integer value) {
        return value != null ? value.longValue() : 0L;
    }

    public static long toLong(Long value) {
        return value != null ? value : 0L;
    }
}
