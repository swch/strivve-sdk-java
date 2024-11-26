package com.strivve;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class RotatorUtilities {

    public static String rotateIntegrator(CardsavrSession session, String integratorName)
            throws CardsavrRESTException, IOException, FileNotFoundException {

        List<AbstractMap.SimpleImmutableEntry<String, String>> filters = new LinkedList<AbstractMap.SimpleImmutableEntry<String, String>>();
        filters.add(new AbstractMap.SimpleImmutableEntry<String, String>("name", integratorName));
        JsonValue response = session.get("/integrators", filters, null);
        if (((JsonArray) response).size() == 0) {
            throw new FileNotFoundException("Unable to rotate unknown integrator: " + integratorName);
        }
        int integratorId = ((JsonArray) response).getJsonObject(0).getJsonNumber("id").intValue();
        response = session.put("/integrators/" + integratorId + "/rotate_key", null, null, null);
        return ((JsonObject)response).getJsonString("current_key").getString();
    }

    public static JsonObject updatePassword(CardsavrSession session, String username, String new_password)
            throws CardsavrRESTException, IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {

        List<AbstractMap.SimpleImmutableEntry<String, String>> filters = new LinkedList<AbstractMap.SimpleImmutableEntry<String, String>>();
        filters.add(new AbstractMap.SimpleImmutableEntry<String, String>("username", username));
        JsonValue response = session.get("/cardsavr_users", filters, null);
        if (((JsonArray) response).size() == 0) {
            throw new FileNotFoundException("Unable to rotate password for unknown username: " + username);
        }
        int userId = ((JsonArray) response).getJsonObject(0).getJsonNumber("id").intValue();

        String path = "/cardsavr_users/" + userId + "/update_password";
        String newPasswordKey = Base64.getEncoder().encodeToString(Encryption.generatePasswordKey(new_password, username));

        JsonObject obj = (JsonObject)Json.createObjectBuilder()
            .add("password", newPasswordKey)
            .add("password_copy", newPasswordKey)
            .build();

        response = session.put(path, null, obj, null);
        return ((JsonObject)response);
    }

}