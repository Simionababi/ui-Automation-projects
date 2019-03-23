package project.pages

import core.Browser
import groovy.util.logging.Slf4j
import org.openqa.selenium.By

@Slf4j
class Search {

    Browser browser

    Search (Browser browser) {
        this.browser = browser
    }

    List<String> searchInGoogleSearch(){
        List<String> errorMessages = []
        String searchBarXpath = "//input[@name='q']"
        String googleSearchButtonLocator = "//input[@name='btnK'][ancestor::div[@jsname='VlcLAe']]"
        String videosHeader = "//h3[text()='Videos']"
        browser.sendKeys(By.xpath(searchBarXpath),"music")
        Thread.sleep(2000)
        browser.click(By.xpath(googleSearchButtonLocator))
        if (!browser.isElementPresent(By.xpath(videosHeader),browser.config.waitForElementPresence.seconds)){
            String errorMessage = "Header 'Videos' is not present on the page"
            errorMessage<<errorMessage
        }
        return errorMessages
    }
}
