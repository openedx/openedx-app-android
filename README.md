# EducationX Android

Modern vision of the mobile application for the Open EdX platform from Raccoon Gang.

[Documentation](Documentation/Documentation.md)

## Building
1. Check out the source code:

        git clone https://github.com/raccoongang/educationx-app-ios.git

2. Open Android Studio and choose Open an Existing Android Studio Project.

3. Choose ``educationx-app-android``.

4. Configure the [config.yaml](config.yaml) with URLs and OAuth credentials for your Open edX instance.

5. Select the build variant ``develop``, ``stage``, or ``prod``.

6. Click the **Run** button.

## API plugin
This project uses custom APIs to improve performance and reduce the number of requests to the server.

You can find the plugin with the API and installation guide [here](https://github.com/raccoongang/mobile-api-extensions).

## License
The code in this repository is licensed under the Apache-2.0 license unless otherwise noted.

Please see [LICENSE](https://github.com/raccoongang/educationx-app-android/blob/main/LICENSE) file for details.
