# Wall Message Service CDT
This project contains the CDT for group Julietts Wall Message Service. It is designed to only use code available in the
Skycave project, and as such, it should be possible to simply copy it over into the Skycave project. Note that you
might have to update some imports.

## Overview
The project contains two Java classes: MessageRecord and CDTWallMessageService.

The MessageRecord class is a simple POJO which was stolen from the Skycave project. It is mainly used for the sake of 
simplicity.

The CDTWallMessageService class is the main class of the project. It contains the actual CDT for the Wall Message 
Service. Each test is structured after the Given-When-Then pattern.

The project is built using Gradle and JUnit 5.

## Getting Started

### Prerequisites
To run the tests, you need to have Java 17 installed on your machine.

### The first run
Please note that the first run might take a while to complete. If you do not already have the image of our 
message-service on your system, it will be downloaded. This might take a while.

It is recommended that you pull our image from Harbor before running the test using the following command:
```shell
docker pull hub.baerbak.com/juliett-public/message-service:latest
```

### Running the tests
To run the tests, simply run the following command in the terminal:
```shell
./gradlew test
```

This will run all the tests in the project.

## Authors
- Anders Bloch, au255459@uni.au.dk
- Mads A. H. Jensen, au758685@uni.au.dk
