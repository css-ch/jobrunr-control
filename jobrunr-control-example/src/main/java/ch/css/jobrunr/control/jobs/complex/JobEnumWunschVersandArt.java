package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.JobEnum;

public enum JobEnumWunschVersandArt {
    @JobEnum(label = "Direktversand", order = 1) DIREKTVERSAND,
    @JobEnum(label = "Portalversand", order = 2) PORTAL_VERSAND
}
