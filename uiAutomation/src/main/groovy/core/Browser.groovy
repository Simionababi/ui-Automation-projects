package core

import core.config.DriverConfig
import core.constants.JsAlert
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.apache.commons.cli.MissingArgumentException
import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.NoAlertPresentException
import org.openqa.selenium.OutputType
import org.openqa.selenium.SearchContext
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import project.properties.PropertyManager
import org.openqa.selenium.Point
import org.openqa.selenium.Dimension

import java.awt.Toolkit
import java.util.concurrent.TimeoutException
import java.util.logging.Level

@Slf4j
class Browser {
    enum DriverMode {
        CHROME, GECKO, EDGE
    }

    DriverConfig config = PropertyManager.driver_config
    private final int PAUSE_TIME_50_MS = 50
    private final int PAUSE_TIME_200_MS = 200
    WebDriver driver = null
    private SearchContext context = null
    private JavascriptExecutor js = null
    def dataContainer

    public <T> T getDataContainerAs(Class<T> clazz) {
        dataContainer = clazz.newInstance()
        return dataContainer
    }

    Browser() {
        driver = setupDriver(config.driver)
    }

    Browser(DriverMode browserMode) {
        driver = setupDriver(browserMode)
    }

    private String getOsDependentDriver(String pathToDriver) {
        String path = pathToDriver
        if (config.OS.contains('Mac OS X')) {
            path = pathToDriver.substring(0, pathToDriver.indexOf(".exe"))
        }
        return path
    }

    private void maximizeScreen() {
        if (config.OS.contains("Mac OS X") && config.driver == DriverMode.CHROME) {
            java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize()
            Point position = new Point(0, 0)
            driver.manage().window().setPosition(position)
            Dimension maximizedScreenSize = new Dimension((int) screenSize.getWidth(), (int) screenSize.getHeight())
            driver.manage().window().setSize(maximizedScreenSize)
        } else {
            driver.manage().window().maximize()
        }
    }

    private ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions()
        options.addArguments(config.commandLineArguments ?: [])
        return options
    }

    private FirefoxOptions getFireFoxOptions() {
        FirefoxOptions options = new FirefoxOptions()
        options.addArguments(config.commandLineArguments ?: [])
        return options
    }

    private WebDriver setupDriver(DriverMode browserMode) {

        switch (browserMode) {
            case DriverMode.CHROME:
                //To do: add logs and add more browser support
                System.setProperty("webdriver.chrome.driver", config.projectRootDirectory + getOsDependentDriver(config.pathToDriverBinary))
                driver = new ChromeDriver()
                break
            case DriverMode.GECKO:
                System.setProperty("webdriver.gecko.driver", config.projectRootDirectory + getOsDependentDriver(config.pathToDriverBinary))
                driver = new FirefoxDriver()
                break
        }
        maximizeScreen()
        js = (JavascriptExecutor) driver
        return driver
    }

    List<String> getElementsText(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        waitForElementPresence(by, timeout)
        return driver.findElements(by).collect { it.getText() }
    }

    List<String> getAttributeValues(By by, String attributeName, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        List<String> attributeValues = []
        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout).seconds
            waitForElementPresence(by, timeout)
            String attributeValue = ""

            driver.findElements(by).eachWithIndex { WebElement element, int index ->
                attributeValue = driver.findElements(by).get(index).getAttribute(attributeName)
                while (new Date().before(expiration) && !atributeValue) {
                    sleep(PAUSE_TIME_50_MS)
                    attributeValue = driver.findElements(by).get(index).getAttribute(attributeName)
                }
                attributeValues << attributeValue
            }
        }
        return attributeValues
    }

    boolean isElementPresent(By by, long timeout = 0) {
        boolean isPresent = driver.findElements(by).size() > 0

        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && !isPresent) {
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
        boolean isElementCovered = (Boolean) find(by, timeout).js(script)

        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && !isElementCovered) {
                sleep(PAUSE_TIME_50_MS)
                isElementCovered = (Boolean) find(by, timeout).js(script)
            }
        }
        return isElementCovered
    }

    Browser find(By by, long timeout = config.waitForElementPresence.seconds) {
        waitForElementPresence(by, timeout)
        context = driver.findElement(by)
        return this
    }

    boolean isAlertPresent(long timeout = 0) {

        def alert = ExpectedConditions.alertIsPresent().apply(driver)
        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && !alert) {
                sleep(PAUSE_TIME_50_MS)
                alert = ExpectedConditions.alertIsPresent().apply(driver)
            }
        }
        boolean isAlert = (alert != null)

        return isAlert
    }

    String getAlertText() {
        String alertText = "The alert box is not present"
        try {
            Alert alert = driver.switchTo().alert()
            alertText = alert.getText()
            alert.accept()
        } catch (NoAlertPresentException nape) {
            log.error(alertText, nape)
        }
        return alertText
    }

    boolean isSelected(By by, long timeout = config.waitForElementPresence.seconds) {
        waitForElementPresence(by, timeout)
        return driver.findElement(by).selected
    }

    int getElementsSize(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        waitForElementPresence(by, timeout)
        SearchContext searchContext = context ?: driver
        int size = searchContext.findElements(by).size()
        context = null
        return size
    }

    void waitForElementPresence(By by, long timeout = config.waitForElementPresence.seconds, boolean terminateContext = true) throws TimeoutException {
        SearchContext searchContext = context ? context : driver

        int size = searchContext.findElements(by).size()
        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && size < 1) {
                sleep(PAUSE_TIME_50_MS)
                size = searchContext.findElements(by).size()
            }
        }
        if (terminateContext) {
            context = null
        }
        if (size < 1) {
            throw new TimeoutException("didn't find element $by in $timeout seconds")
        }
    }

    void waitForElementAbsence(By by, boolean isElementInitiallyPresentOnThePage = true, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        if (!isElementInitiallyPresentOnThePage) {
            waitForElementPresence(by, timeout)
        }
        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && driver.findElements(by).size() > 0) {
                sleep(PAUSE_TIME_50_MS)
            }
        }
        if (driver.findElements(by).size() > 0) {
            throw new TimeoutException("element $by didn't disapear from DOM in $timeout seconds")
        }
    }

    void waitForElementInvisibility(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {

        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            waitForElementPresence(by, timeout)
            while (new Date().before(expiration) && driver.findElement(by).displayed) {
                sleep(PAUSE_TIME_50_MS)
            }
        }
        if (driver.findElement(by).displayed) {
            throw new TimeoutException("element $by didn't become invisible in $timeout seconds")
        } else {
            log.info("element $by become invisible")
        }
    }

    void waitForElementVisibility(By by, long timeout = config.waitForElementPresence.seconds, boolean terminateContext = true) throws TimeoutException {

        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            waitForElementPresence(by, timeout, terminateContext)
            while (new Date().before(expiration) && !driver.findElement(by).displayed) {
                sleep(PAUSE_TIME_50_MS)
            }
        }
        if (!driver.findElement(by).displayed) {
            throw new TimeoutException("element $by didn't become visible in $timeout seconds")
        } else {
            log.info("element $by become visible")
        }
    }

    void sendText(By by, List<String> text, boolean clearInputs = false, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        waitForElementVisibility(by, timeout)
        driver.findElements(by).eachWithIndex { WebElement element, int index ->
            if (clearInputs) {
                element.clear()
            }
            element.sendKeys(text[index])
        }
    }

    void sendKeys(By by = null, CharSequence keys, boolean clearInputs = false, long timeout = config.waitForElementPresence.seconds) {
        if (by) {
            waitForElementVisibility(by, timeout)
            driver.findElements(by).each { element ->
                if (clearInputs) {
                    element.clear()
                    element.sendKeys(keys)
                } else {
                    new Actions(driver).sendKeys(keys).perform()
                }

            }
        }
    }

    void clear(By by, long timeout = config.waitForElementPresence.seconds) {
        waitForElementPresence(by, timeout)
        driver.findElements(by).each { element ->
            element.clear()
        }
    }

    void click(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        waitForElementVisibility(by, timeout, false)
        SearchContext searchContext = context ? context : driver
        WebDriverException exception = null
        try {
            searchContext.findElements(by).each { element ->
                element.click()
            }
        } catch (WebDriverException e) {
            exception = e
        } finally {
            context = null
            if (exception) {
                throw new WebDriverException(exception)
            }
        }
    }

    void waitForElementToBeClickable(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        waitForElementVisibility(by, timeout)
        boolean isClickable = ExpectedConditions.elementToBeClickable(by).apply(driver)

        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout)
            while (new Date().before(expiration) && !isClickable) {
                sleep(PAUSE_TIME_50_MS)
                isClickable = ExpectedConditions.elementToBeClickable(by).apply(driver)
            }
            if (!isClickable) {
                throw new TimeoutException("element $by didn't become clickable in $timeout seconds")
            } else {
                log.info("element $by become clickable")
            }
        }
    }


    void doubleClick(By by, long timeout = config.waitForElementPresence.seconds) throws WebDriverException {
        waitForElementVisibility(by, timeout)
        SearchContext searchContext = context ? context : driver
        WebDriverException exception = null
        try {
            searchContext.findElements(by).each { element ->
                Actions actions = new Actions(driver)
                actions.doubleClick(element).perform()
            }
        } catch (WebDriverException e) {
            exception = e
        } finally {
            context = null
            if (exception)
                throw new WebDriverException(exception)
        }
    }

    void moveToAndClick(By by, Point offset = null, long timeout = config.waitForElementPresence.seconds) {
        waitForElementPresence(by, timeout)
        WebElement element = driver.findElement(by)
        if (offset) {
            new Actions(driver).moveToElement(element, offset.x, offset.y).click().perform()
        } else {
            new Actions(driver).moveToElement(element).click(element).perform()
        }
    }

    void dragAndDrop(By source, By target, Point sourceOffset = null, Point targetOffset = null, long timeout = config.waitForElementPresence.seconds) {
        Actions actions = new Actions(driver)

        //click And Hold
        if (source) {
            waitForElementPresence(source, timeout)
            WebElement sourceElement = driver.findElement(source)

            if (sourceOffset) {
                actions.moveToElement(sourceElement, sourceOffset.x, sourceOffset.y).clickAndHold().perform()
            } else {
                actions.moveToElement(sourceElement).clickAndHold().perform()
            }
        } else {
            if (!sourceOffset) {
                throw new NullPointerException("Eihter source element locator and sourceOffset point or at least one of them should be provided")
            }
            actions.moveByOffset(sourceOffset.x, sourceOffset.y).clickAndHold().perform()
        }
        //drag and drop
        if (target) {
            waitForElementPresence(target, timeout)
            WebElement dropZone = driver.findElement(target)
            if (targetOffset) {
                actions.moveToElement(dropZone, targetOffset.x, targetOffset.y).perform()
                actions.release(dropZone).perform()
            } else {
                actions.moveToElement(dropZone).perform()
                actions.release(dropZone).perform()
            }
        } else {
            if (!targetOffset) {
                throw new NullPointerException("Either target element locator and targetOffset point or at least one of them should be provided.See method documentation")
            }
            actions.moveByOffset(targetOffset.x, targetOffset.y).perform()
            actions.release().perform()
        }
    }

    void select(By by, def value, boolean trimWhiteSpaces = false) {
        waitForElementPresence(by)
        Select select = new Select(driver.findElement(by))
        if (value instanceof Integer) {
            select.selectByIndex(value)
        } else if (value instanceof String) {
            def listOfValues = select.getOptions().collect { webElement -> if (trimWhiteSpaces) return webElement.getAttribute("value").trim() else webElement.getAttribute("value") }
            if (listOfValues.contains(value)) {
                select.selectByIndex(listOfValues.indexOf(value))
            } else {
                select.selectByVisibleText(value)
            }
        } else {
            throw new IllegalArgumentException("${value.class.simpleName} is not allowed. Valid types for select(By by, def value) are String and Integer ")
        }
    }

    void click(JsAlert buttonName) {
        try {
            switch (buttonName) {
                case JsAlert.OK:
                    driver.switchTo().alert().accept()
                    break
                case JsAlert.CANCEL:
                    driver.switchTo().alert().dismiss()
                    break
            }
        } catch (NoAlertPresentException e) {
            log.error("alert is not present!", e)
        }
    }

    void hover(By by, Point offset = null, long timeout = config.waitForElementPresence.seconds){
        waitForElementPresence(by, timeout)
        WebElement element = driver.findElement(by)

        if (offset){
            new Actions(driver).moveToElement(element, offset.x, offset.y).perform()
        }else {
            new Actions(driver).moveToElement(element).perform()
        }
    }

    def js(String script){
        log.info("method js() is executing JS script: '${script.replace("arguments[0]","$context")}'")
        def result
        if (context){
            result = js.executeScript(script, context)
        }else {
            result = js.executeScript(script)
        }
        context= null
        log.info("method js() returs $result")
        return result
    }

    void scroll(By by, Point offset = null, long timeout = config.waitForElementPresence.seconds){
        if (by){
            find(by, timeout).js("argumets[0].scrollIntoView(false)")
        }
        if (offset){
            find(by, timeout).js("window.scroolBy(${offset.x}, ${offset.y})")
        }
    }

    void scrollView(String viewCssSelector, Point offSet, long timeout = config.waitForElementPresence.seconds) {
        log.info("method scrollView() by css selector $viewCssSelector and off-set($offSet)")
        int offsetWidth = offSet.x
        int offsetHeight = offSet.y
        By by = By.cssSelector(viewCssSelector)
        waitForElementPresence(by, timeout)

        if (offsetWidth != 0) {
            js("document.querySelector('$viewCssSelector').scrollLeft=$offsetWidth")
            log.debug("scrolled horizontally by width off-set $offsetWidth")
        }

        if (offsetHeight != 0) {
            js("document.querySelector('$viewCssSelector').scrollTop=$offsetHeight")
            log.debug("scrolled vertically by height off-set $offsetHeight")
        }
    }

    /**
     * this method works only in conjunction with context, e.g. if you want to apply styles to an element you need to chain it with find() method:
     * browser.find(By).setCSS([cssProperties])
     * @param cssProperties is a map of properties [property_name:property_value]
     * @return
     */
    void setCSS(Map cssProperties) {
        log.info("method setCSS()")
        log.info("appling following styles $cssProperties to $context")
        if (context) {
            cssProperties.each { k, v ->
                js.executeScript("arguments[0].style.$k='$v'", context)
            }
        } else
            throw new MissingArgumentException("Context is required but missing. See java doc for setCSS() method")

        context = null
    }

    /**
     * this method works only in conjunction with context, e.g. if you want to get (computed) styles of an element you need to chain it with find() method:
     * browser.find(By).getCSS('width')
     * @param styleName - String defining the name of CSS style need to be retrieved
     * @return
     */
    String getCSS(String styleName) {
        log.info("method getCSS()")
        def styleValue
        log.info("retrieving computed CSS styles from $context")
        if (context) {
            styleValue = (String) js.executeScript("return document.defaultView.getComputedStyle(arguments[0],null).getPropertyValue('$styleName')", context)
        } else
            throw new MissingArgumentException("Context is required but missing. See java doc for getCSS() method")

        context = null
        log.info("method getCSS() returns: $styleValue")
        return styleValue
    }

    void switchFrameTo(int frameNumber, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        log.info("method switchFrameTo() with argument of frame number $frameNumber")
        log.info "waiting for presence of iframe #$frameNumber"
        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout).seconds
            while (new Date().before(expiration) && driver.findElements(By.tagName("iframe")).size() < frameNumber + 1) {
                Thread.sleep(PAUSE_TIME_50_MS)
            }
        }
        if (driver.findElements(By.tagName("iframe")).size() < frameNumber + 1)
            throw new TimeoutException("didn't find element iframe[$frameNumber]")

        driver.switchTo().frame(frameNumber)
        log.info "switched iframe by number: $frameNumber"
    }

    void switchFrameTo(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException {
        log.info("method switchFrameTo() with argument by element: $by")
        waitForElementPresence(by, timeout)
        driver.switchTo().frame(driver.findElement(by))
        log.info "switched iframe by element: $by"
    }

    void switchFrameToTop() {
        log.info("method switchFrameToTop()")
        driver.switchTo().defaultContent()
        log.info "switched to top"
    }

    void switchTabTo(int tabNumber) {
        log.info("method switchTabTo() with argument tab number: $tabNumber")
        driver.switchTo().window(driver.getWindowHandles()[tabNumber])
        log.info "switched to tab #$tabNumber"
    }

    void goTo(String url) {
        log.info "method goTo() with argument url: $url"
        driver.get(url)
    }

    void takeScreenShot(List<String> filePaths = null, String fileName = null) {
        log.info("method takeScreenShot()")
        filePaths = filePaths ?: ["src/test/resources/screenshots"]
        fileName = fileName ?: "${System.currentTimeMillis()}.png"

        File tempFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)
        log.info("created screenshot at ${tempFile.canonicalPath}")

        filePaths.each { filePath ->
            def folder = new File(filePath)

            if (!folder.exists())
                folder.mkdirs()
            if (folder.usableSpace < 1000000) {
                log.info "usable space left less than 1 mb ($folder.usableSpace bytes) skipping the screenshot"
                return
            }

            File file = new File(filePath, fileName)
            Files.createParentDirs(file)
            Files.copy(tempFile, file)

            log.info("copied screenshot to: ${file.canonicalPath}")
        }
    }

    String getPageTitle() {
        log.info("method getPageTitle()")
        return driver.getTitle()
    }

    void refreshPage() {
        log.info("method refreshPage()")
        driver.navigate().refresh()
    }

    /**
     Closes active window/tab (doesn't work in marionette 0.10)
     */
    void close() {
        log.info("method close()")
        driver.close()
    }

    /**
     * closes all the browser instances and shuts down sessions
     */
    void quitAllBrowsers() {
        log.info("method quitAllBrowsers()")
        driver.quit()
        driver = null
    }

    boolean waitUntil(Closure<Boolean> condition, long timeout = config.waitForElementPresence.seconds, boolean throwsException = true) throws TimeoutException {
        log.info("method waitUntil()")
        boolean isTrue = condition()
        use(TimeCategory) {
            Date expiration = new Date() + Math.toIntExact(timeout).seconds
            while (new Date().before(expiration) && !isTrue) {
                Thread.sleep(PAUSE_TIME_200_MS)
                isTrue = condition()
            }
        }

        if (throwsException && !isTrue)
            throw new TimeoutException("Expected condition wasn't satisfied in $timeout seconds")

        log.info("method waitUntil() returns: $isTrue")
        return isTrue
    }

    boolean waitUntilExpectedNumberOfTabs(int expectedTabs, long timeout = config.waitForElementPresence.seconds) {
        int numberOfTabs = getNumberOfTabsOpenedInWindow()

        if (numberOfTabs == expectedTabs) {
            log.info("The number of tabs ($numberOfTabs) equals to the expected $expectedTabs tabs.")
            return true
        } else {
            log.info("The number of tabs ($numberOfTabs) does not equal to the expected $expectedTabs tabs. " +
                    "Waiting for expected number for $timeout seconds")
            waitUntil(
                    { getNumberOfTabsOpenedInWindow() == expectedTabs },
                    timeout,
                    false
            )
        }

        int numberOfTabsAfterWait = getNumberOfTabsOpenedInWindow()

        if (numberOfTabsAfterWait == expectedTabs) {
            log.info("The number of tabs equals to $numberOfTabsAfterWait after the wait")
            return true
        } else {
            log.error("The number of tabs ($numberOfTabsAfterWait) after waiting does not equal to the expected $expectedTabs tabs.")
            return false
        }
    }


    String getCurrentUrl() {
        log.info("method getCurrentUrl()")
        return driver.getCurrentUrl()
    }

    void resetContext(){
        log.info("method resetContext() is resetting context value to null")
        context = null
    }

    void clickAndWaitForUpdate(By by, long timeout = config.waitForPageToLoad.seconds){
        log.info("method clickAndWaitForUpdate()")
        String currentPage = getAttributeValues(By.tagName("body"), "innerHTML", timeout)[0]
        click(by, timeout)
        waitUntil({currentPage != getAttributeValues(By.tagName("body"), "innerHTML", timeout)[0]}, timeout)
    }

    List<String> getConsoleLogs(String logType = LogType.BROWSER, Level logLevel = Level.ALL) {
        log.info("method getConsoleLogs()")
        return driver.manage().logs().get(logType).filter(logLevel).collect{it.message}
    }

    Dimension getDimension(By by, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{
        log.info("method getDimension()")
        waitForElementPresence(by, timeout)
        return driver.findElement(by).getSize()
    }

    List<Point> getListOfPointsForVerticalScrolls(By byView, By byContent, int addVerticalContentPixels = 0, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{
        log.info("method getListOfPointsForVerticalScrolls()")
        List<Point> points = []
        waitForElementPresence(byView, timeout)
        if (!isElementPresent(byContent, timeout)) {
            return points
        }
        Dimension dimView 		= getDimension(byView, timeout)
        Dimension dimContent 	= getDimension(byContent, timeout)

        double pixelsContentHeight = dimContent.height + addVerticalContentPixels

        int numberOfScrolls 	= (int) Math.ceil(pixelsContentHeight / dimView.height) - 1
        int reminder = pixelsContentHeight % dimView.height

        if (numberOfScrolls == 0) {
            points = []
        } else if (numberOfScrolls == 1) {
            points << new Point(0, reminder)
        } else {
            int numOfFullScrolls = numberOfScrolls - 1
            int currectHeight = 0
            numOfFullScrolls.times() {
                currectHeight += dimView.height
                points << new Point(0, currectHeight)
            }
            currectHeight +=reminder
            points << new Point(0, currectHeight)
        }
        log.info("Number of scrolls to see a full view: ${points.size()}. " +
                "Number of vertical pixels of a view element:  ${dimView.height}. " +
                "Number of vertical pixels of a content element: ${dimContent.height} " +
                "and additional vertical pixels: $addVerticalContentPixels" )

        return points
    }

    int getNumberOfVerticalScrolls(By byView, By byContent, int addVerticalContentPixels = 0, long timeout = config.waitForElementPresence.seconds) throws TimeoutException{
        log.info("method getNumberOfScrollsForFullView()")
        waitForElementPresence(byView, timeout)
        waitForElementPresence(byContent, timeout)

        Dimension dimView 		= getDimension(byView, timeout)
        Dimension dimContent 	= getDimension(byContent, timeout)

        int pixelsHeight = dimContent.height + addVerticalContentPixels

        int numberOfScrolls 	= (int) Math.ceil(pixelsHeight / dimView.height) - 1

        log.info("Number of scrolls to see a full view: $numberOfScrolls. " +
                "Number of vertical pixels of a view element:  ${dimView.height}. " +
                "Number of vertical pixels of a content element: ${dimContent.height} " +
                "and additional vertical pixels: $addVerticalContentPixels" )

        return numberOfScrolls
    }

    /**
     * Counts the number of tabs opened in a window
     * @return int denoting the number of tabs opened in a window
     */
    int getNumberOfTabsOpenedInWindow(){
        log.info("method getNumberOfTabsOpenedInWindow()")
        return driver.getWindowHandles().size()
    }



}
