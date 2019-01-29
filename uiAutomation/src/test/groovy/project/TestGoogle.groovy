package project

import config.BaseTest
import core.Browser
import org.testng.annotations.Test
import project.pages.LoginPage

class TestGoogle extends BaseTest{

    @Test
    void navigateToGoogle(){
        LoginPage loginPage = new LoginPage(browser)
        loginPage.navigateToLoginPage()
    }
}
