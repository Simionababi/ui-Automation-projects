package project.pages

import core.Browser
import project.properties.ProjectProperties
import project.properties.PropertyManager

class LoginPage{
    ProjectProperties props = PropertyManager.project_properties
    Browser browser
    LoginPage(Browser browser){
        this.browser = browser
    }

    void navigateToLoginPage(){
        browser.goTo(props.googleUrl)
    }
}
