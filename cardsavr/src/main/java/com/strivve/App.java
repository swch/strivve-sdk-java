package com.strivve;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonValue;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        App app = new App();
        app.go();
    }

    Logger logger;

    public void go() {

        logger = Logger.getLogger(ConsoleHandler.class.getName());

        String username = "testing_user";
        String password = "password";
        String integratorName = "testing_integrator";
        String integratorKey = "OkzxB4HQit43k3OEbSYTAPG5hf96erAAlhb4prAjH/I=";
        String apiServer = "api.localhost.cardsavr.io";

        CardsavrSession.rejectUnauthroized(false);
        CardsavrSession session = CardsavrSession.createSession(integratorName, integratorKey, apiServer);

        try {
            session.login(username, password, null);
            CardsavrSession.APIHeaders headers = session.createHeaders();
            headers.paging = Json.createObjectBuilder().add("page", 1).add("page_length", 5).build();
            List<NameValuePair> filters = new ArrayList<>(1);
            filters.add(new BasicNameValuePair("tags", "canada"));

            JsonValue response = session.get("/merchant_sites", filters, headers);
            logger.info("Error: " + response.toString());
        } catch (CarsavrRESTException e) {
            for (CarsavrRESTException.Error err : e.getRESTErrors()) {
                logger.warning(err.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
