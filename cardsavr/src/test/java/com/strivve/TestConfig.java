package com.strivve;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;

public class TestConfig {

    String integratorName;
    String integratorKey;
    HttpHost cardsavrServer;
    UsernamePasswordCredentials cardsavrCreds;
    HttpHost proxy;
    UsernamePasswordCredentials proxyCreds;

    public static TestConfig getTestConfig() throws FileNotFoundException, MalformedURLException, URISyntaxException {

        TestConfig tc = new TestConfig();
        File f = new File("docker.local.json");
        
        //either load up the config from the docker local file for running the pr-tester
        if (f.exists()) {
            JsonReader reader = Json.createReader(new FileInputStream("docker.local.json"));
            JsonArray config = reader.readArray();
            
            Map<String, String> values = config
                .stream()
                .map(JsonObject.class::cast)
                .collect(Collectors.toMap(j -> j.getString("key"), j -> j.getString("value")));;
            tc.integratorName = values.get("testing/credentials/primary/integrator/name");
            tc.integratorKey = values.get("testing/credentials/primary/integrator/key");
            URL url = new URL("https://" + values.get("cardsavr/config/base_url")); 
            tc.cardsavrServer = new HttpHost(url.getHost(), url.getPort());
            tc.cardsavrCreds = new UsernamePasswordCredentials(
                values.get("testing/credentials/primary/user/username"), 
                values.get("testing/credentials/primary/user/password"));
            tc.integratorKey = values.get("testing/credentials/primary/integrator/key");
        //or simply use the creds file detailed in the creds.sample.json
        } else {
            JsonReader reader = Json.createReader(new FileInputStream("strivve_creds.json"));
            JsonObject creds = reader.readObject();
            reader.close();
            creds = TestConfig.getInstance(creds);
            tc.integratorName = creds.getString("app_name");
            tc.integratorKey = creds.getString("app_key");
            URI uri = new URI(creds.getString("cardsavr_server"));
            tc.cardsavrServer = new HttpHost(uri.getHost(), uri.getPort());
            tc.cardsavrCreds = new UsernamePasswordCredentials(creds.getString("app_username"), creds.getString("app_password"));
            if (creds.containsKey("proxy_server")) {
                tc.proxy = new HttpHost(creds.getString("proxy_server"), creds.getInt("proxy_port"));
                if (creds.containsKey("proxy_username") && creds.containsKey("proxy_password")) {
                    tc.proxyCreds = new UsernamePasswordCredentials(creds.getString("proxy_username"), creds.getString("proxy_password"));
                }
            }
        }
        return tc;
    }

    private static JsonObject getInstance(JsonObject creds) {
        String instance = creds.getString("instance");
        if (instance != null && creds.getJsonArray("instances") != null) {
            for (JsonObject obj : creds.getJsonArray("instances").toArray(
                new JsonObject[creds.getJsonArray("instances").size()])) {
                    if (obj.getString("instance").equals(instance)) {
                        return obj;
                    }
            }
        }
        return creds;
    }
}
