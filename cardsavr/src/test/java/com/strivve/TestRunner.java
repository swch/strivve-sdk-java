package com.strivve;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;

public class TestRunner {

    String integratorName;
    String integratorKey;
    HttpHost cardsavrServer;
    UsernamePasswordCredentials cardsavrCreds;

    public TestRunner() throws FileNotFoundException, MalformedURLException {
        JsonReader reader = Json.createReader(new FileInputStream("docker.local.json"));
        JsonArray config = reader.readArray();
        
        Map<String, String> values = config
            .stream()
            .map(JsonObject.class::cast)
            .collect(Collectors.toMap(j -> j.getString("key"), j -> j.getString("value")));;
        integratorName = values.get("testing/credentials/primary/integrator/name");
        integratorKey = values.get("testing/credentials/primary/integrator/key");
        URL url = new URL(values.get("api_url_override"));
        cardsavrServer = new HttpHost(url.getHost(), url.getPort());
        cardsavrCreds = new UsernamePasswordCredentials(
            values.get("testing/credentials/primary/user/username"), 
            values.get("testing/credentials/primary/user/password"));
        integratorKey = values.get("testing/credentials/primary/integrator/key");
    }
}
