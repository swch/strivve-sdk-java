package com.strivve;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.strivve.CarsavrRESTException.Error;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
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

    @Test
    public void jobPostTest() throws IOException, CarsavrRESTException, InterruptedException {
        String data = new String(Files.readAllBytes(Paths.get("./job_data.json")), StandardCharsets.UTF_8)
                .replaceAll("\\{\\{CARDHOLDER_UNIQUE_KEY\\}\\}", RandomStringUtils.random(6, true, true));
        JsonObject jsonobj = Json.createReader(new StringReader(data)).read().asJsonObject();
        CardsavrSession.APIHeaders headers = this.session.createHeaders();
        headers.financialInsitution = "default";
        JsonObject response = (JsonObject) session.post("/place_card_on_single_site_jobs", jsonobj, headers);
        int jobId = response.getInt("id");
        assertTrue("Place job should return a valid id", response.getInt("id") > 0);

        final CountDownLatch latch = new CountDownLatch(5);

        Timer t = new Timer();
        TimerTask tt = new TimerTask() {
            List<String> assertStatuses = new LinkedList<>();
            boolean addNextStatus = false;

            public void run() {
                JsonObject jobStatus;
                try {
                    jobStatus = (JsonObject) session.get("/place_card_on_single_site_jobs", jobId, null);
                    String status = jobStatus.getString("status");
                    if (addNextStatus) {
                        assertStatuses.add(status);
                        addNextStatus = false;
                    }
                    if (status.equals("PENDING_NEWCREDS")) {
                        JsonObject newCreds = Json.createObjectBuilder()
                            .add("account", Json.createObjectBuilder()
                                .add("username", "good_email")
                                .add("password", "no_tfa")
                                .build())
                            .build();
                        JsonValue obj = session.put("/place_card_on_single_site_jobs", jobId, newCreds, null);
                        addNextStatus = true;
                    } else if (status.equals("PENDING_TFA")) {
                        JsonObject newCreds = Json.createObjectBuilder()
                            .add("account", Json.createObjectBuilder()
                                .add("tfa", "1234")
                                .build())
                            .build();
                        JsonValue obj = session.put("/place_card_on_single_site_jobs", jobId, newCreds, null);
                        addNextStatus = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CarsavrRESTException e) {
                    System.out.println(e.getRESTErrors()[0]);
                    e.printStackTrace();
                }
                latch.countDown();
            }
        };
        t.scheduleAtFixedRate(tt, 1000, 5000);  //wait one second, then wait five
        //System.out.println(tt.assertStatuses);
        latch.await();
        t.cancel();
    }
}
