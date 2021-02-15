package com.strivve;

import static org.junit.Assert.*;

import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.strivve.CarsavrRESTException.Error;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class E2ETest {

    final String username = "testing_user";
    final String password = "password";
    final String integratorName = "testing_integrator";
    final String integratorKey = "OkzxB4HQit43k3OEbSYTAPG5hf96erAAlhb4prAjH/I=";
    final String apiServer = "api.localhost.cardsavr.io";

    CardsavrSession session;

    @Before
    public void rejectUnauthorized() {
        CardsavrSession.rejectUnauthroized(false);
    }

    @Before
    public void loginTest() throws IOException, CarsavrRESTException {
        this.session = CardsavrSession.createSession(integratorName, integratorKey, apiServer);
        JsonObject obj = (JsonObject) session.login(username, password, null);
        assertTrue(obj.getInt("user_id") > 0);
    }

    @Test
    public void selectMerchantsTest() throws IOException, CarsavrRESTException {
        JsonValue response = this.session.get("/merchant_sites", null, null);
        assertEquals(25, ((JsonArray) response).size());
    }

    @Test
    public void selectMerchantsPaginationTest() throws IOException, CarsavrRESTException {
        CardsavrSession.APIHeaders headers = this.session.createHeaders();
        headers.paging = Json.createObjectBuilder().add("page", 1).add("page_length", 5).build();
        JsonValue response = session.get("/merchant_sites", null, headers);
        assertEquals(5, ((JsonArray) response).size());
    }

    @Test
    public void selectMerchantsFilterTest() throws IOException, CarsavrRESTException {
        List<NameValuePair> filters = new ArrayList<>(1);
        filters.add(new BasicNameValuePair("tags", "canada"));
        JsonValue response = session.get("/merchant_sites", filters, null);
        List<String> list = ((JsonArray) response).getJsonObject(0).getJsonArray("tags").stream()
                .map(object -> ((JsonString) object).getString()).collect(Collectors.toList());
        assertTrue(list.contains("canada"));
    }

    @Test
    public void filterError() throws IOException, CarsavrRESTException {
        List<NameValuePair> filters = new ArrayList<>(1);
        filters.add(new BasicNameValuePair("bad_filter", "canada"));
        CarsavrRESTException exception = assertThrows(CarsavrRESTException.class,
                () -> session.get("/merchant_sites", filters, null));
        Error[] errors = exception.getRESTErrors();
        assertEquals(1, errors.length);
        assertTrue(errors[0].toString().endsWith("Property: bad_filter"));
    }

}
