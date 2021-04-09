package com.strivve;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import org.junit.Ignore;

/**
 * Unit test for simple App.
 */
public class E2ETest {

    TestConfig testConfig;

    CardsavrSession session;

    @Before
    public void loadCreds() throws FileNotFoundException, MalformedURLException, URISyntaxException {
        testConfig = TestConfig.getTestConfig();
    }

    @Before
    public void rejectUnauthorized() {
        CardsavrSession.rejectUnauthorized(false);
    }

    @Before
    public void loginTest() throws IOException, CarsavrRESTException {

        this.session = CardsavrSession.createSession(testConfig.integratorName, testConfig.integratorKey, testConfig.cardsavrServer, testConfig.proxy, testConfig.proxyCreds);
        JsonObject obj = (JsonObject) session.login(testConfig.cardsavrCreds, null);
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
        assertTrue("Place job should return a valid id", response.getInt("id") > 0);

        final CountDownLatch latch = new CountDownLatch(1);

        List<String> assertStatuses = new LinkedList<>();
        Timer t = new Timer();
        int delay = 5000;
        t.scheduleAtFixedRate(new TimerTask() {

            private int totalTime = 0;

            public void run() {
                totalTime += delay;
                int jobId = response.getInt("id");
                CardsavrSession.APIHeaders headers = session.createHeaders();
                headers.hydration = Json.createArrayBuilder().add("credential_requests").build();

                try {
                    JsonObject job = (JsonObject) session.get("/place_card_on_single_site_jobs", jobId, headers);
                    String status = job.getString("status");
                    if (!assertStatuses.contains(status)) {
                        assertStatuses.add(status);
                        System.out.println(status);
                    }
                    JsonArray arr = (JsonArray)job.get("credential_requests");
                    if (arr != null && arr.size() == 1 && arr.getJsonObject(0).getString("envelope_id") != null) {

                        headers = session.createHeaders();
                        headers.envelopeId = arr.getJsonObject(0).getString("envelope_id");
                        JsonObject newCreds = null;
                        if (status.equals("PENDING_NEWCREDS")) {
                            newCreds = Json.createObjectBuilder()
                                .add("account", Json.createObjectBuilder()
                                    .add("username", "good_email")
                                    .add("password", "tfa")
                                    .build())
                                .build();
                        } else if (status.equals("PENDING_TFA")) {
                            newCreds = Json.createObjectBuilder()
                                .add("account", Json.createObjectBuilder()
                                    .add("tfa", "1234")
                                    .build())
                                .build();
                            }
                        JsonValue obj = session.put("/place_card_on_single_site_jobs", jobId, newCreds, headers);
                    } else if (!job.get("completed_on").toString().equals("null")) {
                        t.cancel();
                        latch.countDown();
                    }
                    if (totalTime > 600000) {
                        throw new IOException("Task timed out after two minutes, exit.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    latch.countDown();
                } catch (CarsavrRESTException e) {
                    System.out.println(e.getRESTErrors()[0]);
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        }, 1000, delay); //wait one second, then wait five
        latch.await();
        assertEquals("Place job should finish with a status of SUCCESSFUL", "SUCCESSFUL", assertStatuses.get(assertStatuses.size() - 1));
        assertTrue("Status entries length should be at least 5", assertStatuses.size() >= 5);
    }
}
