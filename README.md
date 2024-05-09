# Open edX Android

Modern vision of the mobile application for the Open edX platform from Raccoon Gang.

[Documentation](Documentation/Documentation.md)

## Building

1. Check out the source code:

        git clone https://github.com/openedx/openedx-app-android.git

2. Open Android Studio and choose Open an Existing Android Studio Project.

3. Choose ``openedx-app-android``.

4. Configure `config_settings.yaml` inside `default_config` and `config.yaml` inside sub directories to point to your Open edX configuration. [Configuration Documentation](./Documentation/ConfigurationManagement.md)

5. Select the build variant ``develop``, ``stage``, or ``prod``.

6. Click the **Run** button.

## API
This project targets on the latest Open edX release and rely on the relevant mobile APIs.

If your platform version is older than December 2023, please follow the instructions to use the [API Plugin](./Documentation/APIs_Compatibility.md).

## License

The code in this repository is licensed under the Apache-2.0 license unless otherwise noted.

Please see [LICENSE](https://github.com/openedx/openedx-app-android/blob/main/LICENSE) file for details.
