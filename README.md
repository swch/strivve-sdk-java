# strivve-sdk-java
A lightweight SDK for Java to communicate with Cardsavr

## Overview

Strivve requires TLS for all API communication, but also encrypts payloads and responses to improve security.  The details of the cryptography are [outlined here](https://developers.strivve.com/resources/cryptography/).  In an effort to ease in buidling applications, Strivve has implemented this encryption and signing in a handful of languages.  The Strivve SDK's and API speifications are [documented](https://swch.github.io/slate), but the specific installation details to get you up and running are located per repository.

## Development

The package was developed in VS Code uisng the Maven plugins, and has a set of cryptography unit tests and integration tests.  The first step is to copy the [creds.sample.json](https://github.com/swch/strivve-sdk-java/blob/main/cardsavr/creds.sample.json) to a creds.json file, and attain credentials to a sandbox by contacting developer-support@strivve.com.  You will receive five impportant pieces of information:

- API Server -- The cardsavr server hostname, and https endpoint.
- Integrator name -- To encrypt payloads, the client provides the integrator name to notify the server which form of encyrptin is used. 
- Integrator key -- An associated shared integrator key is used for encyption until ECDH keys can be shared by both parties.  The integrator key is also necessary for generating password keys.
- Application username -- An account is required to authenticate with cardsavr
- Application password -- Password for the corresponding application username

In addition to these setting, there are additional optional proxy settings that can be set up. They can be removed if unnecessary.

- Proxy server
- Proxy port
- Proxy username
- Proxy password

Once your settings are configured, you can build the maven project:

```
mvn package clean
```

This will build the project and run the tests.  Once this is working, you're good to go.

## javax.json

The javax.json package is used for all data handling.  It's understood there are a lot of json packages out there, (e.g. gson), so I'm open to adding support for a different parser.  The SDK is fairly heavily dependent on this package.

## Examples

By refrencing the API documentation, you should be able to locate the relevant entities required.  You can also reference our [postman samples](https://github.com/swch/Strivve-SDK/blob/master/postman-samples/), which walk you through the process of logging in, posting jobs, and polling for results.

```
CardsavrSession session = CardsavrSession.createSession(integratorName, integratorKey, cardsavrServer, proxy, proxyCreds);
JsonObject obj = (JsonObject) session.login(cardsavrCreds, null);
```

Here we simply create a session, and use it to log in.  The resulting object contains information about the agent that logged in.

## Exception handling

All REST calls will respond with errors in the result of bad data being posted.  There is an exception that conveniently wraps the errors.  Simply wrapping all api calls with a try/catch that catches the CardsavrRESTException will provide you the visibility into the errors returned the by server.

```
try {
  session.SOMECALL();
} catch (CardsavrRESTException e) {
  Error[] errors = e.getRESTExceptions();
  //handle
} catch (IOException e) {
  //a connectivity problem as opposed to a data problem.
}
```

## Headers

As documented on the [API documentation site](https://swch.github.io/slate), there are a variety of headers you can use to paginate, and hydrate response entities.  Other headers are also required in certain instances (like safe keys and financial institutions).  Here is an example on how to create a custom headers object and set the value.  The APIHeaders object in CardsavrSession contains all the headers you can optionally set.  Most are optional.

```
CardsavrSession.APIHeaders headers = this.session.createHeaders();
headers.financialInsitution = "default";
JsonObject response = (JsonObject) session.post("/place_card_on_single_site_jobs", jsonobj, headers);
```

