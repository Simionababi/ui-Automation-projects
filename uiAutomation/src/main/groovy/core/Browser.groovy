package core

import core.config.DriverConfig
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.NoAlertPresentException
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import project.properties.PropertyManager

import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import java.sql.Time
import java.util.concurrent.TimeoutException

@Slf4j
class Browser {
    enum DriverMode{CHROME, GECKO, EDGE}

    DriverConfig config = PropertyManager.driver_config
    private final int PAUSE_TIME_50_MS = 50
    private final int PAUSE_TIME_200_MS = 200
    WebDriver driver = null
    private SearchContext context = null
    private JavascriptExecutor js = null
    def dataContainer

    public <T> T getDataContainerAs(Class<T> clazz){
        dataContainer = clazz.newInstance()
        return dataContainer
    }

    Browser(){
        driver = setupDriver(config.driver)
    }
    Browser(DriverMode browserMode){
        driver = setupDriver(browserMode)
    }

    private String getOsDependendDriver(String pathToDriver){
        String path = pathToDriver
        if (config.OS.contains('Mac OS X')){
            path = pathToDriver.substring(0,pathToDriver.indexOf(".exe"))
        }
        return path
    }

    private void maximizeScreen(){
        if (config.OS.contains("Mac OS X") && config.driver == DriverMode.CHROME){
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize()
            Point position = new Point(0,0)
            driver.manage().window().setPosition(position)
            Dimension maximizedScreen = new Dimension(int)screenSize.getWidth(), (int)screenSize.getHeight()
            driver.manage().window().setSize(maximizedScreen)
        }else {
            driver.manage().window().maximize()
        }
    }

    private ChromeOptions getChromeOptions(){
        ChromeOptions options = new ChromeOptions()
        options.addArguments(config.commandLineArguments ?: [])
        return options
    }

    private FirefoxOptions getFireFoxOptions(){
        FirefoxOptions options = new FirefoxOptions()
        options.addArguments(config.commandLineArguments ?: [])
        return options
    }

    private WebDriver setupDriver(DriverMode browserMode){

        switch (browserMode){
            case DriverMode.CHROME:
                //To do: add logs and add more browser support
                System.setProperty("webdriver.chrome.driver", config.projectRootDirectory + getOsDependendDriver(config.pathToDriverBinary))
                driver = new ChromeDriver()
                break
            case DriverMode.GECKO:
                System.setProperty("webdriver.gecko.driver",config.projectRootDirectory + getOsDependendDriver(config.pathToDriverBinary))
                driver = new FirefoxDriver()
                break
        }
        maximizeScreen()
        js = (JavascriptExecutor)driver
        return driver
    }

    List<String> getElementsText(By by, long timeout = config.waitForElementPresence.seconds ) throws TimeoutException{
        waitForElementPresence(by,timeout)
        return driver.findElements(by).collect {it.getText()}
    }

    List<String> getAttributeValues(By by, String attributeName, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{
        List<String> attributeValues = []
        use(TimeCategory){
            Date expiration = new Date() + Math.toIntExact(timeout).seconds
            waitForElementPresence(by, timeout)
            String attributeValue = ""

            driver.findElements(by).eachWithIndex{ WebElement element, int index ->
                attributeValue = driver.findElements(by).get(index).getAttribute(attributeName)
                while (new Date().before(expiration) && !atributeValue){
                    sleep(PAUSE_TIME_50_MS)
                    attributeValue = driver.findElements(by).get(index).getAttribute(attributeName)
                }
                attributeValues << attributeValue
            }
        }
        return attributeValues
    }

    boolean isElementPresent(By by, long timeout = 0){
        boolean isPresent = driver.findElements(by).size() > 0

        use(TimeCategory){
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && !isPresent){
                sleep(PAUSE_TIME_50_MS)
                isPresent = driver.findElements(by).size() > 0
            }
        }
        return isPresent
    }

    boolean isElementCovered(By by, long timeout = 0) {
        String script = """
                        var rect    = arguments[0].getBoundingClientRect()
                        var xCenter    = rect.left + rect.width/2
                        var yCenter   = rect.top + rect.height/2
                        var elementUnderPoint = document.elementFromPoint(xCenter, yCenter)
                        var isElementCovered = !elementUnderPoint.isSameNode(arguments[0])
                        """
        boolean isElementCovered = (Boolean)find(by, timeout).js(script)

        use(TimeCategory){
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && !isElementCovered){
                sleep(PAUSE_TIME_50_MS)
                isElementCovered= (Boolean)find(by,timeout).js(script)
            }
        }
        return isElementCovered
    }

    Browser find(By by, long timeout = config.waitForElementPresence.seconds){
        waitForElementPresence(by,timeout)
        context = driver.findElement(by)
        return this
    }

    boolean isAlertPresent(long timeout = 0){

        def alert = ExpectedConditions.alertIsPresent().apply(driver)
        use(TimeCategory){
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && !alert){
                sleep(PAUSE_TIME_50_MS)
                alert = ExpectedConditions.alertIsPresent().apply(driver)
            }
        }
        boolean isAlert = (alert != null)

        return isAlert
    }

    String getAlertText(){
        String alertText = "The alert box is not present"
        try {
            Alert alert = driver.switchTo().alert()
            alertText = alert.getText()
            alert.accept()
        }catch(NoAlertPresentException nape){
            log.error(alertText, nape)
        }
        return alertText
    }

    boolean isSelected(By by, long timeout = config.waitForElementPresence.seconds){
        waitForElementPresence(by, timeout)
        return driver.findElement(by).selected
    }

    int getElementsSize(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{
        waitForElementPresence(by, timeout)
        SearchContext searchContext = context ?: driver
        int size = searchContext.findElement(by).size
        context = null
        return size
    }

    void waitForElementPresence(By by, long timeout = config.waitForElementPresence.seconds, boolean terminateContext = true) throws TimeoutException{
        SearchContext searchContext = context ? context : driver

        int size = searchContext.findElements(by).size()
        use(TimeCategory){
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && size < 1){
                sleep(PAUSE_TIME_50_MS)
                size = searchContext.findElements(by).size()
            }
        }
        if (terminateContext){
            context = null
        }
        if (size < 1){
            throw new TimeoutException("didn't find element $by in $timeout seconds")
        }
    }

    void waitForElementAbsence(By by, boolean isElementInitiallyPresentOnThePage = true, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{
        if (!isElementInitiallyPresentOnThePage) {
            waitForElementPresence(by, timeout)
        }
            use(TimeCategory){
                Date expiration = new Date() + Math.toIntExact(timeout)
                while (new Date().before(expiration) && driver.findElements(by).size() > 0){
                    sleep(PAUSE_TIME_50_MS)
            }
        }
        if (driver.findElements(by).size() > 0){
            throw new TimeoutException("element $by didn't disapear from DOM in $timeout seconds")
        }
    }

    void waitForElementInvisibility(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{

        use(TimeCategory){
            Date expiration = new Date() + Math.toIntExact(timeout)
            waitForElementPresence(by, timeout)
            while (new Date().before(expiration) && driver.findElement(by).displayed){
                sleep(PAUSE_TIME_50_MS)
            }
        }
        if (driver.findElement(by).displayed){
            throw new TimeoutException("element $by didn't become invisible in $timeout seconds")
        }else {
            log.info("element $by become invisible")
        }
    }

    void waitForElementVisibility(By by, long timeout= config.waitForElementPresence.seconds, boolean terminateContext = true) throws TimeoutException{

        use(TimeCategory){
            Date expiration = new Date() + Math.toIntExact(timeout)
            waitForElementPresence(by, timeout,terminateContext)
            while (new Date().before(expiration) && !driver.findElement(by).displayed){
                sleep(PAUSE_TIME_50_MS)
            }
        }
        if (!driver.findElement(by).displayed){
            throw new TimeoutException("element $by didn't become visible in $timeout seconds")
        }else {
            log.info("element $by become visible")
        }
    }

    void sendText(By by, List<String> text, boolean clearInputs = false, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{
        waitForElementVisibility(by, timeout)
        driver.findElements(by).eachWithIndex{ WebElement element, int index ->
            if (clearInputs){
                element.clear()
            }
            element.sendKeys(text[index])
        }
    }

    void sendKeys(By by = null, CharSequence keys, boolean  clearInputs = false, timeout = config.waitForElementPresence.seconds){
        if (by){
            waitForElementVisibility(by, timeout)
            driver.findElements(by).each {element ->
                if (clearInputs){
                    element.clear()
                    element.sendKeys(keys)
                }else {
                    new Actions(driver).sendKeys(keys).perform()
                }

            }
        }
    }


}
