# zepp-fit-extractor

## Why this application

If like me you are moving from amazfit device to a garmin device you may want to keep all your previous history in the new garmin ecosystem.
Zepp bulk export only provides CSV files and unfortunately the fit export can only be done on single activities.

This project has been created in order to be able to extract activities to `.fit` files in order to keep at much data as possible.
Call to remote 'Zepp webservices' has been implemented based on the work of [Mi Fit and Zepp workout exporter]((https://github.com/rolandsz/Mi-Fit-and-Zepp-workout-exporter) github project.

## Compilation

* Maven and Java 17 are needed

`mvn clean install`

## Usage

### How to get the authentication token

1. Open the [GDPR page](https://user.huami.com/privacy2/index.html?loginPlatform=web&platform_app=com.xiaomi.hm.health)
2. Click `Export data`
3. Sign in to your account
4. Open the developer tools in your browser (F12)
5. Select the `Network` tab
6. Click on `Export data` again
7. Look for any request containing the `apptoken` header or cookie

TODO

## Next step

* Manage more sport
    * Swimming
    * cycling
    * Walk
    * ...
* Add shell command to manipulate the application
