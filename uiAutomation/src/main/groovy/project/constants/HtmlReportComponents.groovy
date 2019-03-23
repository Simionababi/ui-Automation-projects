package project.constants

import static project.constants.HtmlReportComponents.Tag.*
import static project.constants.HtmlReportComponents.Entity.*

class HtmlReportComponents {

    interface Entity{
        String NON_BREAKING_SPAKE = '&nbsp'
        String THREE_SPACES = "$NON_BREAKING_SPAKE$NON_BREAKING_SPAKE$NON_BREAKING_SPAKE"
        String FOUR_SPACES  = "$NON_BREAKING_SPAKE$NON_BREAKING_SPAKE$NON_BREAKING_SPAKE$NON_BREAKING_SPAKE"
    }

    interface Lines{
        String nl = System.lineSeparator();
        String tb = "\t" //tab
    }

    interface Tag{
        String BR ='<br/>'
        String SPAN_OPEN_COLOR_RED_TEXT = "<span style='color:red; font-wight:bold;'>"
        String SPAN_CLOSE = '</span>'
        String SPAN_OPEN_COLOR_GREEN_TEXT = "<span style='color:green; font-wight:bold;'>"
    }
}
