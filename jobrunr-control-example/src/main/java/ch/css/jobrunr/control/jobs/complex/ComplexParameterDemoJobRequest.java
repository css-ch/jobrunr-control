package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobParameterSection;
import ch.css.jobrunr.control.annotations.JobParameterSectionLayout;
import jakarta.validation.constraints.Pattern;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.time.LocalDate;

public record ComplexParameterDemoJobRequest(
        @JobParameterSection(id = "filter",
                title = "Filter-Parameter",
                order = 1,
                layout = JobParameterSectionLayout.TWO_VALUES_ON_LINE_LABEL_ABOVE)
        @JobParameterDefinition(displayName = "Hauptfälligkeit",
                defaultValue = "",
                sectionId = "filter",
                order = 1,
                description = "Filtert die Druckaufträge nach einem bestimmten Hauptfälligkeits-Datum.")
        Hauptfaelligkeit filterHauptfaelligkeit,
        @JobParameterDefinition(displayName = "Korrespondenzsprache KE",
                defaultValue = "",
                sectionId = "filter",
                order = 2)
        Sprache filterKorrespondenzspracheKe,
        @JobParameterDefinition(displayName = "Wunschversand-Art",
                defaultValue = "",
                sectionId = "filter",
                order = 3)
        WunschVersandArt filterWunschVersandArt,
        @JobParameterDefinition(displayName = "PolicenNrs Einschluss",
                type = "MULTILINE",
                defaultValue = "",
                sectionId = "filter",
                order = 4,
                description = "Filtert die Druckaufträge nach den angegebenen Policen-Nr(s). Es können mehrere Policen-Nr(s) als Zeilenumbruch- oder kommaseparierte Liste eingegeben werden. Es werden nur Druckaufträge berücksichtigt, die eine der angegebenen Policen-Nr(s) enthalten.")
        @Pattern(regexp = "[0-9]*",
                message = "Ungültiges Format für PoliceNrs-Einschluss")
        String filterPoliceNrsEinschluss,
        @JobParameterDefinition(displayName = "PolicenNrs Ausschluss",
                type = "MULTILINE",
                defaultValue = "",
                sectionId = "filter",
                order = 5)
        String filterPoliceNrsAusschluss,
        @JobParameterDefinition(displayName = "Policen-Block-Ids Einschluss",
                type = "MULTILINE",
                defaultValue = "",
                sectionId = "filter",
                order = 6)
        String filterPolicenListeBlockIdsEinschluss,
        @JobParameterDefinition(displayName = "Policen-Block-Ids Ausschluss",
                type = "MULTILINE",
                defaultValue = "",
                sectionId = "filter",
                order = 7)
        String filterPolicenListeBlockIdsAusschluss,
        @JobParameterDefinition(displayName = "BatchJobNr Besteller-Batch",
                type = "MULTILINE",
                defaultValue = "",
                sectionId = "filter",
                order = 8)
        String filterBatchJobNrsBestellerBatches,
        @JobParameterSection(id = "steuerung",
                title = "Steuerungs-Parameter",
                order = 2,
                layout = JobParameterSectionLayout.TWO_VALUES_ON_LINE_LABEL_ABOVE)
        @JobParameterDefinition(displayName = "Zustellung Portal ab",
                defaultValue = "",
                sectionId = "steuerung",
                order = 1)
        LocalDate steuerungZustellungPortalAb,
        @JobParameterDefinition(displayName = "Physischer Druck von PortalVersand",
                defaultValue = "false",
                sectionId = "steuerung",
                order = 2)
        Boolean steuerungPhysischerDruckPortalVersand,
        @JobParameterDefinition(displayName = "Drucksprache übersteuern (für Test)",
                defaultValue = "false",
                sectionId = "steuerung",
                order = 3)
        Sprache steuerungUebersteuerndeSprache,
        @JobParameterDefinition(displayName = "Beilagen-Nrs",
                defaultValue = "",
                sectionId = "steuerung",
                order = 4,
                description = "Geben Sie hier die Beilagen-Nr(s) an, die zusätzlich im Brief mitgesendet werden. Es können mehrere Beilagen-Nr(s) als Kommaseparierte Liste eingegeben werden.")
        String steuerungBeilageNrs
) implements JobRequest {
    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return ComplexParameterDemoJob.class;
    }
}
