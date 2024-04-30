# zepp-fit-extractor

## Why this application

If like me you are moving from amazfit device to a garmin device you may want to keep all your previous history in the new garmin ecosystem.
Zepp bulk export only provides CSV files and unfortunately the fit export can only be done on single activities.

This project has been created in order to be able to extract activities to `.fit` files in order to keep at much data as possible.
Call to remote 'Zepp webservices' has been implemented based on the work of [Mi Fit and Zepp workout exporter](https://github.com/rolandsz/Mi-Fit-and-Zepp-workout-exporter) github project.

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

## Tips

### How to check fit file content

A useful website to see user data and developer data of a fit file : https://www.fitfileviewer.com/

### How to delete multiple import on garmin connect website

```js
//go to activity page first. Use and advanced search to filter elements to delete. Click search and past code into console
//When page is clear, you can refresh you search manually and the scrip will continue

var jq = document.createElement('script');
jq.src = "https://ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js";
document.getElementsByTagName('head')[0].appendChild(jq);
// ... give time for script to load
jQuery.noConflict();

function foo() {
    var li = $(".list-item").not('.fadeOut')[0];
    var delButton = $(li).find("button.js-activity-delete");
    var confirmDelButton = $(li).find("button.delete-yes");

    $(delButton).click();

    setTimeout(function() {
      $(confirmDelButton).click();
    }, 100);
}

setInterval(foo, 500);
```
