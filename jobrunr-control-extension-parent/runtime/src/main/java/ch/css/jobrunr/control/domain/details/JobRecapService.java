package ch.css.jobrunr.control.domain.details;

import java.util.Map;

public interface JobRecapService {
    void writeRecap(Map<String,Long> recap);
}
