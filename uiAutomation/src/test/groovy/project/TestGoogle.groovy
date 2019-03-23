package project

import com.relevantcodes.extentreports.ExtentTest
import com.relevantcodes.extentreports.LogStatus
import org.testng.annotations.Test
import project.pages.LoginPage
import project.pages.Search

class TestGoogle extends BaseTestGoogle{

    ExtentTest outerTest, innerTest
    String currStep, currDtls
    @Test
    void navigateToGoogle(){
        outerTest = extentReports.startTest("Navigate to ${props.googleUrl}")
        innerTest = extentReports.startTest("Open google search page")
        currStep = "Check if the page opens"
        currDtls = "enviroment $env"
        LoginPage loginPage = new LoginPage(browser)
        loginPage.navigateToLoginPage()
        innerTest.log(LogStatus.INFO, currStep,currDtls)
        outerTest.appendChild(innerTest)
        extentReports.endTest(innerTest)

        innerTest = extentReports.startTest("Search for music in google search bar")
        currStep = "Check if can search for music in google google search bar"
        currDtls = "Verifying that can search for music in google search bar "
        innerTest.log(LogStatus.INFO,currStep, currDtls)

        Search search = new Search(browser)
        List<String> errorMessages = search.searchInGoogleSearch()
        if (errorMessages){
            innerTest.log(LogStatus.FAIL,currStep, currDtls)
        }else {
            innerTest.log(LogStatus.PASS,currStep, currDtls)
        }
        outerTest.appendChild(innerTest)
        extentReports.endTest(innerTest)
        extentReports.endTest(outerTest)
        extentReports.flush()

    }
}
