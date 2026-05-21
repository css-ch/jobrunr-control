package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobParameterSection;
import ch.css.jobrunr.control.annotations.JobParameterSectionLayout;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ComplexParameterDemoJobParameter(
    @JobParameterSection(id = "filter1",
            title = "Filter-Parameter",
            order = 1,
            layout = JobParameterSectionLayout.SINGLE_VALUE_ON_LINE_LABEL_OBOVE)
    @JobParameterDefinition(displayName = "Hauptfälligkeit",
            sectionId = "filter1",
            order = 1,
            required = false,
            defaultValue = "",
            description = "Filtert die Druckaufträge nach der Hauptfälligkeit. Es werden nur Druckaufträge berücksichtigt, deren Hauptfälligkeit mit dem angegebenen Wert übereinstimmt.")
    JobEnumSpvHauptfaelligkeit filterHauptfaelligkeit,

    @JobParameterDefinition(displayName = "Korrespondenzsprache",
            sectionId = "filter1",
            order = 2,
            required = false,
            defaultValue = "",
            description = "Es werden nur Druckaufträge berücksichtigt, deren Korrespondenzsprache des Korrespondenzempfängers mit der angegebenen Sprache übereinstimmt.")
    JobEnumSprache filterKorrespondenzspracheKe,

    @JobParameterDefinition(displayName = "Wunschversandart",
            sectionId = "filter1",
            order = 3,
            required = false,
            defaultValue = "")
    JobEnumWunschVersandArt filterWunschVersandArt,

    @JobParameterSection(id = "filter2",
            title = "",
            order = 2,
            layout = JobParameterSectionLayout.TWO_VALUES_ON_LINE_LABEL_ABOVE)
    @JobParameterDefinition(type = "MULTILINE",
            displayName = "PolicenNrs Einschluss",
            sectionId = "filter2",
            order = 1,
            maxlength = 20000,
            required = false,
            defaultValue = "",
            description = "Filtert die Druckaufträge nach den angegebenen Policen-Nr(s). Es können mehrere Policen-Nr(s) als Zeilenumbruch- oder kommaseparierte Liste eingegeben werden. Es werden nur Druckaufträge berücksichtigt, die eine der angegebenen Policen-Nr(s) enthalten.")
    @PoliceNrListValidation
    String filterPoliceNrsEinschluss,

    @JobParameterDefinition(type = "MULTILINE",
            displayName = "PolicenNrs Ausschluss",
            sectionId = "filter2",
            order = 2,
            maxlength = 20000,
            required = false,
            defaultValue = "",
            description = "Filtert die Druckaufträge nach den angegebenen Policen-Nr(s). Es können mehrere Policen-Nr(s) als Zeilenumbruch- oder kommaseparierte Liste eingegeben werden. Alle angegebenen Policen werden nicht verarbeitet, auch dann nicht, wenn sie unter Einschluss angegeben wurden.")
    @PoliceNrListValidation
    String filterPoliceNrsAusschluss,

    @JobParameterDefinition(type = "MULTILINE",
            displayName = "Policen-Block-Ids Einschluss",
            sectionId = "filter2",
            order = 3,
            required = false,
            defaultValue = "",
            description = "Filtert die Druckaufträge nach den Policen-Nrs in den angegebenen Policen-Block-Ids. Es können mehrere Policen-Block-Ids als Zeilenumbruch- oder kommaseparierte Liste eingegeben werden. Es werden nur Druckaufträge berücksichtigt, dere Policen-Nr in einem der angegebenen Policen-Block enthalten ist.")
    @Pattern(regexp = "^(\\s*[a-z0-9-]{3,20}(\\s*[,;\\s]+\\s*[a-z0-9-]{3,20})*)?\\s*$", message = "Ungültiges Format für PolicenListeBlockIds. Erlaubt sind eine oder mehrere PolicenListeBlockId (Format: 3 bis 20 Zeichen aus a-z, 0-9 oder -), getrennt durch Komma, Semikolon, Leerzeichen oder Zeilenumbruch.")
    String filterPolicenListeBlockIdsEinschluss,

    @JobParameterDefinition(type = "MULTILINE",
            displayName = "Policen-Block-Ids Ausschluss",
            sectionId = "filter2",
            order = 4,
            required = false,
            defaultValue = "",
            description = "Filtert die Druckaufträge nach den Policen-Nrs in den angegebenen Policen-Block-Ids. Es können mehrere Policen-Block-Ids als Zeilenumbruch- oder kommaseparierte Liste eingegeben werden. Alle in den Policen-Block's angegebenen Policen werden nicht verarbeitet, auch dann nicht, wenn sie unter Einschluss angegeben wurden.")
    @Pattern(regexp = "^(\\s*[a-z0-9-]{3,20}(\\s*[,;\\s]+\\s*[a-z0-9-]{3,20})*)?\\s*$", message = "Ungültiges Format für PolicenListeBlockIds. Erlaubt sind eine oder mehrere PolicenListeBlockId (Format: 3 bis 20 Zeichen aus a-z, 0-9 oder -), getrennt durch Komma, Semikolon, Leerzeichen oder Zeilenumbruch.")
    String filterPolicenListeBlockIdsAusschluss,

    @JobParameterDefinition(type = "MULTILINE",
            displayName = "BatchJobNrn der Besteller-Batche",
            sectionId = "filter2",
            order = 5,
            required = false,
            defaultValue = "")
    @Pattern(regexp = "^(\\s*[a-zA-Z0-9.\\\\-_]{1,36}(\\s*[,;\\s]+\\s*[a-zA-Z0-9.\\\\-_]{1,36})*)?\\s*$", message = "Ungültiges Format für BatchJobNrn. Erlaubt sind eine oder mehrere BatchJobNr (Format: 1 bis 36 Zeichen aus a-z, A-Z, 0-9 oder .-_), getrennt durch Komma, Semikolon, Leerzeichen oder Zeilenumbruch.")
    String filterBatchJobNrsBestellerBatches,


    @JobParameterSection(id = "steuerung",
            title = "Steuerungs-Parameter",
            order = 2,
            layout = JobParameterSectionLayout.TWO_VALUES_ON_LINE_LABEL_ABOVE)
    @JobParameterDefinition(displayName = "Zustellung Portal ab",
            sectionId = "steuerung",
            order = 1,
            defaultValue = "",
            required = false,
            description = "Die PRAN-Police wird erst ab dem 'Zustellung Portal ab'-Datum im Portal ersichtlich sein.")
    LocalDate steuerungZustellungPortalAb,

    @JobParameterDefinition(displayName = "Physischer Druck bei Portalversand erzwingen",
            sectionId = "steuerung",
            order = 2,
            required = true,
            defaultValue = "false",
            description = "Mit dieser Optional kann erzwungen werden, dass die Druckaufträge, die eigentlich über das Portal versendet werden sollten, physisch gedruckt und nicht über das Portal versendet werden.")
    Boolean steuerungPhysischerDruckPortalVersand,

    @JobParameterDefinition(displayName = "Beilage-Nrn",
            sectionId = "steuerung",
            order = 3,
            defaultValue = "",
            required = false,
            description = "Kommaseparierte Liste mit Beilagen-Nr(s), welche zusätzlich mit der Police versendet werden.")
    @Pattern(regexp = "[^*]{1,128}", message = "Ungültiges Format für Beilage-Nr(s). Erlaubt sind eine oder mehrere Beilage-Nr(s) (maximal 128 Zeichen, keine Sternchen), getrennt durch Komma.")
    String steuerungBeilageNrs,

    @JobParameterDefinition(displayName = "Übersteuern der Dokumentensprache (zu Testzwecken)",
            sectionId = "steuerung",
            order = 4,
            defaultValue = "",
            required = false,
            description = "Diese Sprache übersteuert die Korrespondenzsprache der Druckaufträge. Sie kann z.B. zu Testzwecken verwendet werden, um die Sprache der Dokumente zu steuern, unabhängig von der im Korrespondenzempfänger hinterlegten Korrespondenzsprache.")
    JobEnumSprache steuerungUebersteuerndeSprache
){}
