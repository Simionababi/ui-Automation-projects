package config

import core.Browser
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod

class BaseTest {
    private ThreadLocal<Browser> browser = new ThreadLocal<Browser>()

    protected Browser getBrowser(){
        return browser.get()
    }

    @BeforeMethod
    void browserLaunch(){
        browser.set(new Browser())
    }

    @AfterMethod
    void browserShutDown(){
        browser.get().quitAllBrowsers()
    }
}
