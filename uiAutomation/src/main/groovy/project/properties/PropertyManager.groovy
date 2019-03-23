package project.properties

import core.config.DriverConfig
import org.aeonbits.owner.ConfigCache

class PropertyManager {
    //Do this for every each properties files and it is related to DriverConfig interface
    //Need to create such interface for every each config file and project
    static DriverConfig driver_config = ConfigCache.getOrCreate(DriverConfig)
    static ProjectProperties project_properties = ConfigCache.getOrCreate(ProjectProperties)

}
