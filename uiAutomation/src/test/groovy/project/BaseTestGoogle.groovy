package project

import com.relevantcodes.extentreports.ExtentReports
import config.BaseTest
import core.utils.TimeStamp
import groovy.util.logging.Slf4j
import org.testng.annotations.BeforeMethod
import project.properties.ProjectProperties
import project.properties.PropertyManager

import java.lang.reflect.Method
@Slf4j
class BaseTestGoogle extends BaseTest{
    ExtentReports extentReports
    String testResultFolder
    String htmlReportPath
    String configExtentReportPath
    String timeStamp = TimeStamp.getTodaysDate("yyyy'-'MM'-'dd'_'HH-mm")
    ProjectProperties props = PropertyManager.project_properties
    String env = props.env

    @BeforeMethod
    void setUpHtmlReport(Method method){
        String userDir = System.getProperty("user.dir")
        testResultFolder = "${method.name}_$timeStamp"
        htmlReportPath = userDir + "/src/test/resources/google/ExtentReport.html"
        configExtentReportPath = userDir + "/src/main/resources/configExtentReports/configExtentReports.xml"

        extentReports = new ExtentReports(htmlReportPath)
        extentReports.loadConfig(new File(configExtentReportPath))
        extentReports
                .addSystemInfo("Browser", "${browser.config.driver}")
                .addSystemInfo("Environment",env)
    }
}
