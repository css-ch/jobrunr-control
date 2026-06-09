package ch.css.jobrunr.control.domain.details;

public interface JobMessageService {

    void info(String message, Object... args);

    void warning(String message, Object... args);

    void error(String message, Object... args);

    void exception(String message, Throwable throwable);
    void exception(String message, Object args1, Throwable throwable);
    void exception(String message, Object args1, Object args2, Throwable throwable);
    void exception(String message, Object args1, Object args2, Object args3, Throwable throwable);
    void exception(String message, Object args1, Object args2, Object args3, Object args4, Throwable throwable);
}
