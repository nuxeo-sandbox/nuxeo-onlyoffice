package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.compress.utils.Sets;

public interface OnlyOfficeTypes {

    String SPREADSHEET = "spreadsheet";

    Set<String> SPREADSHEET_TYPES = Collections.unmodifiableSet(
            Sets.newHashSet("application/vnd.ms-excel", "application/vnd.ms-excel.sheet.macroenabled.12",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel.template.macroenabled.12",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.template"));

    String TEXT = "text";

    Set<String> TEXT_TYPES = Collections.unmodifiableSet(
            Sets.newHashSet("application/msword", "application/vnd.ms-word.document.macroenabled.12",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-word.template.macroenabled.12",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.template"));

    String PRESENTATION = "presentation";

    Set<String> PRESENTATION_TYPES = Collections.unmodifiableSet(Sets.newHashSet("application/vnd.ms-powerpoint",
            "application/vnd.ms-powerpoint.presentation.macroenabled.12",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint.slideshow.macroenabled.12",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            "application/vnd.ms-powerpoint.template.macroenabled.12",
            "application/vnd.openxmlformats-officedocument.presentationml.template",
            "application/vnd.openxmlformats-officedocument.presentationml.slide"));

}
