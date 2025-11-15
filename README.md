# zepp-fit-extractor

## Why this application

If like me you are moving from amazfit/xiaomi device to a garmin device you may want to keep all your previous history in the new garmin connect platform.
Zepp bulk export only provides CSV files and unfortunately the fit export can only be done on single activities one by one with the application.

This project has been created to be able to extract activities to `.fit` files in order to keep as much data as possible.
Call to remote 'Zepp webservices' has been implemented based on the work of [Mi Fit and Zepp workout exporter](https://github.com/rolandsz/Mi-Fit-and-Zepp-workout-exporter) github project.

## Sports supported

Here a table of supported sports:

| Sport Supported | Version |
|-----------------|---------|
| Running         | 0.1.0   |
| Walking         | 0.1.0   |
| Cycling         | 0.1.0   |
| Indoor swimming | 0.1.0   |
| Treadmill       | 0.1.1   |

For now laps are not supported (not included in the output files).

The mapping from Zepp to `.fit` file has been realized based on my personal data. Input devices were :

* Mi band 4
* Mi band 5
* Amazfit stratos 3

That's why some data may have not been handled on included in output `.fit` because they were not in my inputs.

## Usage

Java 17 needed.

### Get your authentication token

1. Open the [GDPR page](https://user.huami.com/privacy2/index.html?loginPlatform=web&platform_app=com.xiaomi.hm.health)
2. Click `Export data`
3. Sign in to your account
4. Open the developer tools in your browser (F12)
5. Select the `Network` tab
6. Click on `Export data` again
7. Look for any request containing the `apptoken` header or cookie

### Start and use the application

Now start the application : `java -jar ./target/zepp-fit-extractor*.jar`.
On the console, you can type `help` to print the available commands.

```commandline
shell:>help
AVAILABLE COMMANDS

Built-In Commands
       help: Display help about available commands
       clear: Clear the shell screen.
       quit, exit: Exit the shell.
       script: Read and execute commands from a file.

Exporter Commands
       generate-single: Generate a single activity based on the trackId
       download-all: Download all sport activities from remote web services
       generate-all: Generate all activities .fit file using previous downloaded resources.
       generate-sport: Generate a activities for a provided sport type
```

For details about a command you can use `help <command>` (ie `help download-all`).
For the main use case, you need to :

1. Download all resources linked with you account (`download-all`)
2. Generate all fit files (`generate-all`)

## Change log

### 0.1.0

First version of the project. Lap record are not manage. Some sport are supported.

* Running
* Cycling
* Walking
* Indoor swimming

## Roadmap

* Manage lap
* Include more sport
* Be able to authenticate user to avoid manual token extraction
* Move from command line to GUI ?

## Tips

### How to check fit file content

A useful website to see user data and developer data of a fit file : https://www.fitfileviewer.com/

This tool is helpful to know what kind of data has been stored and to compare from other fit file as referential.

### How to delete multiple import on garmin connect website

I encounter a problem when I had imported multiple files at once but improvement were possible on this activities, I don't find a way to bulk delete activities on garmin connect website.
Here a script found on a gist, to be able ot automate the click on the delete button.

Take care to first filter activities you want to delete.

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
