package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.JobEnum;

public enum JobEnumSpvHauptfaelligkeit {
    @JobEnum(label = "Januar (01.01.)", order = 1) JANUARY,
    @JobEnum(label = "Februar (01.02.)", order = 2) FEBRUARY,
    @JobEnum(label = "März (01.03.)", order = 3) MARCH,
    @JobEnum(label = "April (01.04.)", order = 4) APRIL,
    @JobEnum(label = "Mai (01.05.)", order = 5) MAY,
    @JobEnum(label = "Juni (01.06.)", order = 6) JUNE,
    @JobEnum(label = "Juli (01.07.)", order = 7) JULY,
    @JobEnum(label = "August (01.08.)", order = 8) AUGUST,
    @JobEnum(label = "September (01.09.)", order = 9) SEPTEMBER,
    @JobEnum(label = "Oktober (01.10.)", order = 10) OCTOBER,
    @JobEnum(label = "November (01.11.)", order = 11) NOVEMBER,
    @JobEnum(label = "Dezember (01.12.)", order = 12) DECEMBER
}
