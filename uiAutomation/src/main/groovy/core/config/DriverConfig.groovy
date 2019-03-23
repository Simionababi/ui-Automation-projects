package core.config

import core.Browser
import org.aeonbits.owner.converters.DurationConverter
import java.time.Duration
import core.config.utils.MapConverter
import core.config.utils.ToUpperCaseProcessor
import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.ConverterClass
import org.aeonbits.owner.Config.Key
import org.aeonbits.owner.Config.DefaultValue
import org.aeonbits.owner.Config.LoadPolicy
import org.aeonbits.owner.Config.PreprocessorClasses
import org.aeonbits.owner.Config.Sources

import java.time.Duration


@LoadPolicy(Config.LoadType.MERGE)
@Sources(["system:properties", "classpath:properties/driver.properties"])

public interface DriverConfig extends Config{
    @Key("driver")
    @DefaultValue("chrome")
    @PreprocessorClasses(ToUpperCaseProcessor)
    Browser.DriverMode getDriver();

    @Key("hubUrl")
    URL getHubUrl();

    @Key("smtp-host")
    @DefaultValue("smtp.gmail.com")
    String getSmtpHost();

    @Key("waitForElement")
    @DefaultValue("30 s")
    @ConverterClass(DurationConverter)
    Duration getWaitForElementPresence();

    @Key("waitForPageToLoad")
    @DefaultValue("4 m")
    @ConverterClass(DurationConverter)
    Duration getWaitForPageToLoad();

    @Key('webdriver.${driver}.driver')
    String getPathToDriverBinary();

    @Key('${driver}.command-line.arguments')
    List<String> getCommandLineArguments();

    @Key("chrome.experimental.options")
    @ConverterClass(MapConverter)
    Map getChromeExperimentalOptionsMap();

    @Key("os.name")
    String getOS();

    @Key("user.dir")
    String getProjectRootDirectory();


}