# Web Phishing Monitoring Framework

## About the Project

Java based open source application that uses selenium webdriver, headless firefox and browsermob-proxy to track content and metadata from phishing websites.

## Build

This project is built using Maven 3 and Java 11.
To build it, in the root directory of this repository run:
```sh
mvn package
```

It will create the directory `target`, containing the file
`WebPhishingFramework.jar` and the sub directory `lib` with all the
dependencies that are automatically installed by Maven.

## Run
This project depends on Selenium `3.141.59`.
Also, since the web page inspection uses the Firefox Web Browser with
Geckodriver, ensure that both of these two dependencies are installed
in the proper compatible versions.
To check the compatibility list, please visit
[Geckdriver Support](https://firefox-source-docs.mozilla.org/testing/geckodriver/Support.html).

After installing Firefox and Geckdriver, build the project as shown in
[Build](#build).

Then, from the root directory, the application can be run as: 
```sh
java -jar target/WebPhishingFramework.jar path_to_config_file.json
```

Notice that JSON file must be passed as an execution argument.
This is supposed to have all the required execution parameters to run the
application.
We've preserved a configuration environment in the folder
[example](example).
So, to run the example configuration environment, call the application
as follows:
```sh
java -jar target/WebPhishingFramework.jar ./example/config.json
```

To understant the configuration environment, go to
[Execution Environment](#execution-environment).

## Execution Environment

### Configuration file
It is a JSON file containing an object with five parameters:
* concurrentBrowsers: Number of concurrent browser instances.
* pageTimeout: Page timeout.
* windowTimeout: Time window for request limiting.
* maxRequests: Request limit per defined time window.
* flag: IDK

See the exemple configuration file at [config.json](example/config.json).

### Input directories
TODO

### Log directories
TODO 

## License
Distributed under MIT license.
