package ch.css.jobrunr.control.domain.details;

public interface JobMessageService {

    void info(String message, Object... args);

    void warning(String message, Object... args);

    void error(String message, Object... args);

    void exception(Throwable throwable);
}
