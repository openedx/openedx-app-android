# Configuration Management

This documentation provides a comprehensive solution for integrating and managing configuration files in Open edX Android project.

## Features
- Parsing config.yaml files
- Adding essential keys to `AndroidManifest.xml` (e.g. Microsoft keys)
- Generating Android build config fields.
- Generating config.json file to use the configuration fields at runtime.
- Generating google-services.json with Firebase keys.

Inside the `Config.kt`, parsing and populating relevant keys and classes are done, e.g. `AgreementUrlsConfig.kt` and `FirebaseConfig.kt`.

## Getting Started

### Configuration Setup

Edit the `config_settings.yaml` in the `default_config` folder. It should contain data as follows:

```yaml
config_directory: '{path_to_config_folder}'
config_mapping:
  prod: 'prod'
  stage: 'stage'
  dev: 'dev'
# These mappings are configurable, e.g. dev: 'prod_test'
```

- `config_directory` provides the path of the config directory.
- `config_mappings` provides mappings that can be utilized to map the Android Build Variant to a defined folder within the config directory, and it will be referenced.

Note: You can specify `config_directory` to any folder outside the repository to store the configs as a separate project.

### Configuration Files
In the `default_config` folder, select your environment folder: prod, stage, dev or any other you have created.
Open `config.yaml` and fill in the required fields.

Example:

```yaml
API_HOST_URL: 'https://mylmsexample.com'
APPLICATION_ID: 'org.openedx.app'
ENVIRONMENT_DISPLAY_NAME: 'MyLMSExample'
FEEDBACK_EMAIL_ADDRESS: 'support@mylmsexample.com'
OAUTH_CLIENT_ID: 'YOUR_OAUTH_CLIENT_ID'

PLATFORM_NAME: "MyLMS"
TOKEN_TYPE: "JWT"

FIREBASE:
  ENABLED: false
  CLOUD_MESSAGING_ENABLED: false
  PROJECT_NUMBER: ''
  PROJECT_ID: ''
  APPLICATION_ID: ''
  API_KEY: ''

MICROSOFT:
  ENABLED: false
  CLIENT_ID: 'microsoftClientID'
```

Also, all environment folders contain a `file_mappings.yaml` file that points to the config files to be parsed.

By modifying `file_mappings.yaml`, you can achieve splitting of the base `config.yaml` or add additional configuration files.

Example:

```yaml
android:
  files:
    - auth_client.yaml
    - config.yaml
    - feature_flags.yaml
```

## Available Third-Party Services
- **Firebase:** Analytics, Crashlytics, Cloud Messaging
- **Google:** Sign in and Sign up via Google
- **Microsoft:** Sign in and Sign up via Microsoft
- **Facebook:** Sign in and Sign up via Facebook
- **Branch:** Deeplinks
- **Braze:** Cloud Messaging

## Available Feature Flags
- **PRE_LOGIN_EXPERIENCE_ENABLED:** Enables the pre login courses discovery experience.
- **WHATS_NEW_ENABLED:** Enables the "What's New" feature to present the latest changes to the user.
- **SOCIAL_AUTH_ENABLED:** Enables SSO buttons on the SignIn and SignUp screens.
- **COURSE_DROPDOWN_NAVIGATION_ENABLED:** Enables an alternative navigation through units.
- **COURSE_UNIT_PROGRESS_ENABLED:** Enables the display of the unit progress within the courseware.
- **REGISTRATION_ENABLED:** Enables user registration from the app.

## Future Support
- To add config related to some other service, create a class, e.g. `ServiceNameConfig.kt`, to be able to populate related fields.
- Create a `function` in the `Config.kt` to be able to parse and use the newly created service from the main Config.

Example:

```Kotlin
fun getServiceNameConfig(): ServiceNameConfig {
    return getObjectOrNewInstance(SERVICE_NAME_KEY, ServiceNameConfig::class.java)
}
```

```yaml
SERVICE_NAME:
  ENABLED: false
  KEY: ''
```

The `default_config` directory is added to the project to provide an idea of how to write config YAML files.
