package ch.css.jobrunr.control.jobs.recap;

import ch.css.jobrunr.control.annotations.JobEnum;

public enum JobEnumWunschVersandArt {
    @JobEnum(label = "Direktversand", order = 1) DIREKTVERSAND,
    @JobEnum(label = "Portalversand", order = 2) PORTAL_VERSAND
}
