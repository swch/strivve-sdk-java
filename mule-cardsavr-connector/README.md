# CardSavr Mule Connector

## Description
This project is intended to be a proof of concept for the use of the CardSavr Java SDK within a MuleSoft connector.  

## Installation

1. Add the Java SDK as a dependency for this project
2. Add credentials to the strivve_creds.json file
3. Add this project as a dependency to you Mule project
```xml
<groupId>cs.muleConnector</groupId>
<artifactId>mule-cardsavr-connector</artifactId>
<version>1.0.0</version>
<classifier>mule-plugin</classifier>
```

## Example Operations

Example operations are provided for creating and deleting cardholders, cards, and addresses.  There is also job creation operation that will create a test job within the CardSavr instance.
