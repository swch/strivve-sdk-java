package com.strivve;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.io.Serializable;

class CarsavrRESTException extends Exception {

    private static final long serialVersionUID = 968681638809960414L;

    private final Error[] errors;

    private final transient JsonValue rawResponse;

    public CarsavrRESTException(String message, JsonArray errorsArray, JsonValue response) {
        super(message);
        this.rawResponse = response;

        List<Error> list = new LinkedList<>();
        errorsArray.forEach(it -> {
            JsonObject obj = it.asJsonObject();
            list.add(new Error(obj.getString("name"), obj.getString("message"), obj.getString("property")));
        });
        this.errors = list.toArray(new Error[list.size()]);
    }

    public Error[] getRESTErrors() {
        return errors;
    }

    public JsonValue getRawResponse() {
        return rawResponse;
    }

    class Error implements Serializable {

        private static final long serialVersionUID = 820230784671482138L;
        private String name;
        private String message;
        private String property;
        
        public Error(String name, String message, String property) {
            this.name = name;
            this.message = message;
            this.property = property;
        }

        public String toString() {
            return "Name: " + name + ", Message: " + message +  ", Property: " + property;
        }
    }

}