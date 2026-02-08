package ch.css.jobrunr.control.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BatchProgress")
class BatchProgressTest {

    @Test
    @DisplayName("getProgress should calculate percentage correctly")
    void shouldCalculatePercentageCorrectly() {
        // Arrange
        BatchProgress progress = new BatchProgress(100, 50, 20);

        // Act
        double result = progress.getProgress();

        // Assert
        assertThat(result).isEqualTo(70.0); // (50 + 20) / 100 * 100 = 70%
    }

    @Test
    @DisplayName("getProgress should return 0 when total is 0")
    void shouldReturnZeroWhenTotalIsZero() {
        // Arrange
        BatchProgress progress = new BatchProgress(0, 0, 0);

        // Act
        double result = progress.getProgress();

        // Assert
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getProgress should return 100 when all complete")
    void shouldReturn100WhenAllComplete() {
        // Arrange
        BatchProgress progress = new BatchProgress(100, 100, 0);

        // Act
        double result = progress.getProgress();

        // Assert
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getPending should calculate pending items correctly")
    void shouldCalculatePendingCorrectly() {
        // Arrange
        BatchProgress progress = new BatchProgress(100, 50, 20);

        // Act
        long pending = progress.getPending();

        // Assert
        assertThat(pending).isEqualTo(30); // 100 - 50 - 20 = 30
    }

    @Test
    @DisplayName("getProcessed should return sum of succeeded and failed")
    void shouldReturnProcessedCount() {
        // Arrange
        BatchProgress progress = new BatchProgress(100, 50, 20);

        // Act
        long processed = progress.getProcessed();

        // Assert
        assertThat(processed).isEqualTo(70); // 50 + 20 = 70
    }

    @Test
    @DisplayName("toString should format correctly")
    void shouldFormatToStringCorrectly() {
        // Arrange
        BatchProgress progress = new BatchProgress(100, 50, 20);

        // Act
        String result = progress.toString();

        // Assert
        assertThat(result)
                .contains("total=100")
                .contains("succeeded=50")
                .contains("failed=20");
    }

    @Test
    @DisplayName("should throw when total is negative")
    void shouldThrowWhenTotalIsNegative() {
        // Act & Assert
        assertThatThrownBy(() -> new BatchProgress(-1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Values must not be negative");
    }

    @Test
    @DisplayName("should throw when sum exceeds total")
    void shouldThrowWhenSumExceedsTotal() {
        // Act & Assert
        assertThatThrownBy(() -> new BatchProgress(100, 60, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sum of succeeded and failed must not exceed total");
    }
}
