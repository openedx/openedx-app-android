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
    }

    def generateMicrosoftConfig() {
        def config = fetchConfig()
        def applicationId = config.getOrDefault("APPLICATION_ID", "")
        def clientId = ""
        def packageSign = ""
        def microsoft = config.get("MICROSOFT")
        if (microsoft) {
            packageSign = microsoft.getOrDefault("PACKAGE_SIGNATURE", "")
            clientId = microsoft.getOrDefault("CLIENT_ID", "")
        }
        def microsoftConfigsJsonPath = projectDir.path + "/core/src/main/res/raw/"
        new File(microsoftConfigsJsonPath).mkdirs()
        def sign = URLEncoder.encode(packageSign, "UTF-8")
        def configJson = [
                client_id                     : clientId,
                authorization_user_agent      : "DEFAULT",
                redirect_uri                  : "msauth://$applicationId/$sign",
                account_mode                  : "MULTIPLE",
                broker_redirect_uri_registered: false
        ]
        new FileWriter(microsoftConfigsJsonPath + "/microsoft_auth_config.json").withWriter {
            it.write(new JsonBuilder(configJson).toPrettyString())
        }
    }

    def generateGoogleServicesJson(applicationId) {
        def config = fetchConfig()
        def firebase = config.get("FIREBASE")
        if (!firebase) {
            return
        }
        if (!firebase.getOrDefault("ENABLED", false)) {
            return
        }

        def googleServicesJsonPath = projectDir.path + "/app/"
        new File(googleServicesJsonPath).mkdirs()

        def projectInfo = [
                project_number: firebase.getOrDefault("PROJECT_NUMBER", ""),
                project_id    : firebase.getOrDefault("PROJECT_ID", ""),
                storage_bucket: "${firebase.getOrDefault("PROJECT_ID", "")}.appspot.com"
        ]
        def clientInfo = [
                mobilesdk_app_id   : firebase.getOrDefault("APPLICATION_ID", ""),
                android_client_info: [
                        package_name: applicationId
                ]
        ]
        def client = [
                client_info : clientInfo,
                oauth_client: [],
                api_key     : [[current_key: firebase.getOrDefault("API_KEY", "")]],
                services    : [
                        appinvite_service: [
                                other_platform_oauth_client: []
                        ]
                ]
        ]
        def configJson = [
                project_info         : projectInfo,
                client               : [client],
                configuration_version: "1"
        ]

        new FileWriter(googleServicesJsonPath + "/google-services.json").withWriter {
            it.write(new JsonBuilder(configJson).toPrettyString())
        }
    }

    def removeGoogleServicesJson() {
        def googleServicesJsonPath = projectDir.path + "/app/google-services.json"
        def file = new File(googleServicesJsonPath)
        if (file.exists()) {
            file.delete()
        }
    }
}
