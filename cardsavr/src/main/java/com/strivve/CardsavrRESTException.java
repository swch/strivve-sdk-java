package com.strivve;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.io.Serializable;

public class CardsavrRESTException extends Exception {

    private static final long serialVersionUID = 968681638809960414L;

    private final Error[] errors;

    private final transient JsonValue rawResponse;

    public CardsavrRESTException(String message, JsonArray errorsArray, JsonValue response) {
        super(message);
        this.rawResponse = response;

        List<Error> list = new LinkedList<>();
        errorsArray.forEach(it -> {
            JsonObject topError = it.asJsonObject();
            list.add(new Error(topError.getString("name"), topError.getString("message"), topError.getString("property"), "top"));
        });
        JsonObject obj = response.asJsonObject();
        obj.forEach((key, value) -> {
            if (value instanceof JsonObject) {
                JsonArray subErrors = ((JsonObject)value).getJsonArray("_errors");
                if (subErrors != null) {
                    subErrors.forEach(it -> {
                        JsonObject subError = it.asJsonObject();
                        list.add(new Error(subError.getString("name"), subError.getString("message"), subError.getString("property"), key));
                    });
                }
            }
        });
        this.errors = list.toArray(new Error[list.size()]);
    }

    public Error[] getRESTErrors() {
        return errors;
    }

    public JsonValue getRawResponse() {
        return rawResponse;
    }

    public class Error implements Serializable {

        private static final long serialVersionUID = 820230784671482138L;
        private String name;
        private String message;
        private String property;
        private String entity;
        
        public Error(String name, String message, String property, String entity) {
            this.name = name;
            this.message = message;
            this.property = property;
            this.entity = entity;
        }

        public String toString() {
            return "Name: " + name + ", Message: " + message +  ", Property: " + property + ", SubEntity: " + entity;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }

        public String getProperty() {
            return property;
        }

        public String getEntity() {
            return entity;
        }
    }

}