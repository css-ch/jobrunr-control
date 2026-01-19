package ch.css.jobrunr.control.domain;

import java.util.Objects;

/**
 * Represents the progress of a batch job.
 * Contains total count as well as the number of successful and failed subjobs.
 */
public class BatchProgress {

    private final long total;
    private final long succeeded;
    private final long failed;

    public BatchProgress(long total, long succeeded, long failed) {
        if (total < 0 || succeeded < 0 || failed < 0) {
            throw new IllegalArgumentException("Values must not be negative");
        }
        if (succeeded + failed > total) {
            throw new IllegalArgumentException("Sum of succeeded and failed must not exceed total");
        }
        this.total = total;
        this.succeeded = succeeded;
        this.failed = failed;
    }

    public long getTotal() {
        return total;
    }

    public long getSucceeded() {
        return succeeded;
    }

    public long getFailed() {
        return failed;
    }

    public long getPending() {
        return total - succeeded - failed;
    }

    public long getProcessed() {
        return succeeded + failed;
    }

    public double getProgress() {
        if (total == 0) {
            return 0.0;
        }
        double percentage = (double) (succeeded + failed) / total * 100.0;
        return Math.round(percentage * 10.0) / 10.0; // Round to 1 decimal place
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchProgress that = (BatchProgress) o;
        return total == that.total && succeeded == that.succeeded && failed == that.failed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, succeeded, failed);
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

