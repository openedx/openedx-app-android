package org.edx.builder

import org.yaml.snakeyaml.Yaml

class ConfigHelper {
    def CONFIG_SETTINGS_YAML_FILENAME = 'config_settings.yaml'
    def DEFAULT_CONFIG_PATH = './default_config/' + CONFIG_SETTINGS_YAML_FILENAME
    def CONFIG_DIRECTORY = "config_directory"
    def CONFIG_MAPPING = "config_mapping"
    def MAPPINGS_FILENAME = 'file_mappings.yaml'
    def ANDROID_CONFIG = "android"
    def FILES_CONFIG = "files"

    def configDir = ""

    ConfigHelper(environment) {
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
            throw new Exception("Inappropriate config directory format")
        }
        def configMappingFiles = new Yaml().load(configFilesMapping.newInputStream())
        def androidFiles = configMappingFiles.getOrDefault(ANDROID_CONFIG, {}).getOrDefault(FILES_CONFIG, [])
        def androidConfigs = new LinkedHashMap()
        androidFiles.each { file ->
            def configFile = new File(configDir + "/" + file)
            if (configFile.exists()) {
                def config = new Yaml().load(configFile.newInputStream())
                androidConfigs.putAll(config)
            }
        }
        return androidConfigs
    }

    FirebaseConfig getFirebaseConfig(config) {
        FirebaseConfig firebaseConfig = new FirebaseConfig()
        if (config["FIREBASE"] != null) {
            firebaseConfig.projectId = setValue(config["FIREBASE"]["PROJECT_ID"])
            firebaseConfig.appId = setValue(config["FIREBASE"]["APPLICATION_ID"])
            firebaseConfig.apiKey = setValue(config["FIREBASE"]["API_KEY"])
            firebaseConfig.gcmSenderId = setValue(config["FIREBASE"]["GCM_SENDER_ID"])
        }
        return firebaseConfig
    }

    private String setValue(value) {
        def result
        if (value == null) {
            result = ""
        } else {
            result = value
        }
        return result
    }
}
