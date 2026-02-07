package ch.css.jobrunr.control.domain;

/**
 * Represents the progress of a batch job.
 * Contains total count as well as the number of successful and failed subjobs.
 *
 * @param total     Total number of items
 * @param succeeded Number of successful items
 * @param failed    Number of failed items
 */
public record BatchProgress(long total, long succeeded, long failed) {

    public BatchProgress {
        if (total < 0 || succeeded < 0 || failed < 0) {
            throw new IllegalArgumentException("Values must not be negative");
        }
        if (succeeded + failed > total) {
            throw new IllegalArgumentException("Sum of succeeded and failed must not exceed total");
        }
    }

    /**
     * Returns the number of pending items.
     */
    public long getPending() {
        return total - succeeded - failed;
    }

    /**
     * Returns the number of processed items (succeeded + failed).
     */
    public long getProcessed() {
        return succeeded + failed;
    }

    /**
     * Returns the progress as a percentage (0.0 to 100.0).
     */
    public double getProgress() {
        if (total == 0) {
            return 0.0;
        }
        double percentage = (double) (succeeded + failed) / total * 100.0;
        return Math.round(percentage * 10.0) / 10.0; // Round to 1 decimal place
    }

    @Override
    public String toString() {
        return "BatchProgress{" +
                "total=" + total +
                ", succeeded=" + succeeded +
                ", failed=" + failed +
                ", pending=" + getPending() +
                ", progress=" + String.format("%.2f", getProgress()) + "%" +
                '}';
    }
}

