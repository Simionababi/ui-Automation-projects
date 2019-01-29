package project.properties
import org.aeonbits.owner.Config.Key
import org.aeonbits.owner.Config

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources(["system:properties","classpath:properties/project.properties"])
interface ProjectProperties extends Config{

    @Key("google.env")
    String getEnv()

    @Key('google.${google.env}.url')
    String getGoogleUrl()


}