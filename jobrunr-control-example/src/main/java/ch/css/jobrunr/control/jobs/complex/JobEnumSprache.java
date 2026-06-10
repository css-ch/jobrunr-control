package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.JobEnum;

public enum JobEnumSprache {

    @JobEnum(label = "Deutsch", order = 1) DEUTSCH,
    @JobEnum(label = "Französisch", order = 2) FRANZOESISCH,
    @JobEnum(label = "Italienisch", order = 3) ITALIENISCH,
    @JobEnum(label = "Englisch", order = 4) ENGLISCH
}