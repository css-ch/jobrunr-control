package ch.css.jobrunr.control.domain;

import ch.css.jobrunr.control.annotations.JobParameterSectionLayout;

/**
 * Represents a section of job parameters in the UI, containing an id, title, order, and layout information.
 *
 * @param id    the unique identifier for the job parameter section
 * @param title the display title for the job parameter section
 * @param order the order in which this section should be displayed relative to other sections
 * @param layout the layout type for displaying the parameters in this section
 */
public record JobParameterSection(String id,
                                  String title,
                                  int order,
                                  JobParameterSectionLayout layout) {
}
