package org.edx.builder

import groovy.json.JsonBuilder
import org.yaml.snakeyaml.Yaml

class ConfigHelper {
    def CONFIG_SETTINGS_YAML_FILENAME = 'config_settings.yaml'
    def DEFAULT_CONFIG_PATH = './default_config/' + CONFIG_SETTINGS_YAML_FILENAME
    def CONFIG_DIRECTORY = "config_directory"
    def CONFIG_MAPPING = "config_mapping"
    def MAPPINGS_FILENAME = 'file_mappings.yaml'
    def ANDROID_CONFIG_KEY = "android"
    def FILES_CONFIG_KEY = "files"

    def configDir = ""
    def projectDir = ""

    ConfigHelper(projectDir, buildType) {
        def environment
        if (buildType == 'develop' || buildType == '') {
            environment = 'dev'
        } else {
            environment = buildType
        }
        this.projectDir = projectDir
        def configFile = new File(CONFIG_SETTINGS_YAML_FILENAME)
        if (!configFile.exists()) {
            // parse default configurations if `config_settings.yaml` doesn't exist
            println("Configurations are missing at " + configFile.path)
            println("Parsing Default configurations from " + DEFAULT_CONFIG_PATH)
            configFile = new File(DEFAULT_CONFIG_PATH)
        }
        if (!configFile.exists()) {
            throw new Exception("Configurations are missing at " + configFile.path)
        }

        def config = new Yaml().load(configFile.newInputStream())
        if (config[CONFIG_DIRECTORY] && config[CONFIG_MAPPING][environment]) {
            configDir = config[CONFIG_DIRECTORY] + "/" + config[CONFIG_MAPPING][environment]
        } else {
            throw new Exception(environment + "key doesn't exist in " + configFile.path)
        }
    }

    def fetchConfig() {
        def configFilesMapping = new File(configDir + "/" + MAPPINGS_FILENAME)
        if (!configFilesMapping.exists()) {
            throw new Exception("Inappropriate config directory format: $configFilesMapping")
        }
        def configMappingFiles = new Yaml().load(configFilesMapping.newInputStream())
        def androidConfigFiles = configMappingFiles.getOrDefault(ANDROID_CONFIG_KEY, {}).getOrDefault(FILES_CONFIG_KEY, [])
        def androidConfigs = new LinkedHashMap()
        androidConfigFiles.each { file ->
            def configFile = new File(configDir + "/" + file)
            if (configFile.exists()) {
                def config = new Yaml().load(configFile.newInputStream())
                androidConfigs.putAll(config)
            }
        }
        return androidConfigs
    }

    def generateConfigJson() {
        def config = fetchConfig()
        def configJsonDir = new File(projectDir.path + "/core/assets/config")
        configJsonDir.mkdirs()
        def jsonWriter = new FileWriter(configJsonDir.path + "/config.json")
        def builder = new JsonBuilder(config)
        jsonWriter.withWriter {
            builder.writeTo(it)
        }
        generateMicrosoftConfig(config)
    }

    private def generateMicrosoftConfig(config) {
        def social = config.get("SOCIAL")
        def applicationId = config.getOrDefault("APPLICATION_ID", "")
        def clientId = social.getOrDefault("MICROSOFT_CLIENT_ID", "")
        def packageSign = social.getOrDefault("MICROSOFT_PACKAGE_SIGNATURE", "")
        def microsoftConfigsJsonPath = projectDir.path + "/core/src/main/res/raw/"
        new File(microsoftConfigsJsonPath).mkdirs()
        def sign = URLEncoder.encode(packageSign, "UTF-8")
        def configJson = [
                client_id                     : clientId,
                redirect_uri                  : "msauth://$applicationId/$sign",
                account_mode                  : "SINGLE",
                broker_redirect_uri_registered: true
        ]
        new FileWriter(microsoftConfigsJsonPath + "/microsoft_auth_config.json").withWriter {
            it.write(new JsonBuilder(configJson).toPrettyString())
        }
    }
}
