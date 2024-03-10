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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.strivve.CardsavrRESTException.Error;
import com.strivve.RotatorUtilities;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;

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
    public void loginTest() throws IOException, CardsavrRESTException {

        this.session = CardsavrSession.createSession(testConfig.integratorName, testConfig.integratorKey, testConfig.cardsavrServer, testConfig.proxy, testConfig.proxyCreds);
        JsonObject obj = (JsonObject) session.login(testConfig.cardsavrCreds, null);
        assertTrue(obj.getInt("user_id") > 0);
    }

    @Test
    public void sessioinRestoreTest() {
        try {
            byte[] sessionObjects = session.serializeSessionObjects();
            CardsavrSession session2 = CardsavrSession.createSession(testConfig.integratorName, testConfig.integratorKey, testConfig.cardsavrServer, testConfig.proxy, testConfig.proxyCreds);
            session2.restore(sessionObjects);
            try {
                JsonValue response = session2.get("/merchant_sites", null, null);
                assertEquals(25, ((JsonArray)response).size());
            } catch (CardsavrRESTException e) {
                e.printStackTrace(); assert(false);
            }
        } catch (IOException e) {
            e.printStackTrace(); assert(false);
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); assert(false);
        }
    }

    @Test
    public void selectMerchantsTest() throws IOException, CardsavrRESTException {
        JsonValue response = this.session.get("/merchant_sites", null, null);
        assertEquals(25, ((JsonArray) response).size());
    }

    @Test
    public void selectMerchantsPaginationTest() throws IOException, CardsavrRESTException {
        CardsavrSession.APIHeaders headers = this.session.createHeaders();
        headers.paging = Json.createObjectBuilder().add("page", 1).add("page_length", 5).build();
        JsonValue response = session.get("/merchant_sites", null, headers);
        assertEquals(5, ((JsonArray) response).size());
    }

    @Test
    public void selectMerchantsFilterTest() throws IOException, CardsavrRESTException {
        List<NameValuePair> filters = new ArrayList<>(1);
        filters.add(new BasicNameValuePair("tags", "canada,development"));
        filters.add(new BasicNameValuePair("tags", "canada,synthetic"));
        JsonValue response = session.get("/merchant_sites", filters, null);
        List<String> list = ((JsonArray) response).getJsonObject(0).getJsonArray("tags").stream()
                .map(object -> ((JsonString) object).getString()).collect(Collectors.toList());
        assertTrue(list.contains("canada"));
    }

    @Test
    public void selectNonExistentMerchantSite() throws IOException, CardsavrRESTException {
        try {
            session.get("/merchant_sites/0", null, null);
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().indexOf("404 Not Found") != -1);
        }
    }

    @Test
    public void badCardRequestTest() throws IOException, CardsavrRESTException {
        try {
            String data = new String(Files.readAllBytes(Paths.get("./job_data.json")), StandardCharsets.UTF_8)
                .replaceAll("\\{\\{CARDHOLDER_UNIQUE_KEY\\}\\}", RandomStringUtils.random(6, true, true));
            data = data.replaceAll("\"subnational\":\"[\\w\\.\\- ]+\",", "");
            JsonObject jsonobj = Json.createReader(new StringReader(data)).read().asJsonObject();
            JsonObject card = jsonobj.getJsonObject("card");
            session.put("/cardsavr_cards", null, card, this.session.createHeaders());
        } catch (CardsavrRESTException e) {
            assertTrue(e.getMessage().indexOf("400") != -1);
            assertTrue(e.getRESTErrors().length == 2);
            Arrays.stream(e.getRESTErrors()).forEach(error -> {
                if (error.getEntity().equals("top")) {
                    assertTrue(error.getName().equals("Sub-Entity Validation Failure"));
                } else if (error.getEntity().equals("address")) {
                    assertTrue(error.getProperty().equals("subnational"));
                }
            });

        }
    }

    @Test
    public void filterError() throws IOException, CardsavrRESTException {
        List<NameValuePair> filters = new ArrayList<>(1);
        filters.add(new BasicNameValuePair("bad_filter", "canada"));
        CardsavrRESTException exception = assertThrows(CardsavrRESTException.class,
                () -> session.get("/merchant_sites", filters, null));
        Error[] errors = exception.getRESTErrors();
        assertEquals(1, errors.length);
        assertTrue(errors[0].toString().indexOf("Property: bad_filter") != -1);
    }

    @Test
    public void createCardWithCardholderGrant() throws IOException, CardsavrRESTException, NoSuchAlgorithmException {
        JsonObject response = null;
        try {
            CardsavrSession.APIHeaders headers = this.session.createHeaders();
            headers.hydration = Json.createArrayBuilder().add("cardholder").build();
            String data = new String(Files.readAllBytes(Paths.get("./card_data.json")), StandardCharsets.UTF_8)
                .replaceAll("\\{\\{CARDHOLDER_UNIQUE_KEY\\}\\}", RandomStringUtils.random(6, true, true));
            JsonObject card = Json.createReader(new StringReader(data)).read().asJsonObject();
            response = (JsonObject)session.post("/cardsavr_cards", card, headers);
        } catch (CardsavrRESTException e) {
            System.out.println(e.getRESTErrors()[0]);
            assert(false); return;
        } catch (IOException e) {
            e.printStackTrace();
            assert(false); return;
        }
        int cardId = response.getInt("id"); // card_id
        String grant = response.getJsonObject("cardholder").getString("grant");
        assertTrue("Create card should return a valid id", cardId > 0);
        assertTrue("Create card with cardholder should return a valid grant", grant != null);
    }

    @Test
    public void testIntegratorPasswordRotation() throws IOException, CardsavrRESTException, NoSuchAlgorithmException {

        CardsavrSession.APIHeaders headers = this.session.createHeaders();
        JsonObject response = null;
        int userId = -1;
        int integratorId = -1;
        try {
            // create an empty user
            String username = "TEST_USER";
            String password = "PASSWORD";
            JsonObject newUser = Json.createObjectBuilder()
                .add("username", username)
                .add("password", Base64.getEncoder().encodeToString(Encryption.generatePasswordKey(password, username)))
                .add("role", "customer_agent")
                .build();
            response = (JsonObject) session.post("/cardsavr_users", newUser, headers);
            userId = response.getInt("id");
            assertTrue(response.getInt("id") > 0);

            // create an integrator
            String integratorName = "TEST_INTEGRATOR";
            JsonObject integrator = Json.createObjectBuilder()
                .add("name", integratorName)
                .add("integrator_type", "application")
                .build();
            response = (JsonObject) session.post("/integrators", integrator, headers);
            integratorId = response.getInt("id");

            // log in as new user with new integrator
            CardsavrSession cs = CardsavrSession.createSession(integratorName, response.getString("current_key"), testConfig.cardsavrServer, testConfig.proxy, testConfig.proxyCreds);
            response = (JsonObject) cs.login(new UsernamePasswordCredentials(username, password), null);
            assertTrue(response.getInt("user_id") == userId);

            String newKey = RotatorUtilities.rotateIntegrator(session, integratorName);
            String newPassword = "PASSWORD_2";
            RotatorUtilities.updatePassword(session, username, newPassword);

            cs = CardsavrSession.createSession(integratorName, newKey, testConfig.cardsavrServer, testConfig.proxy, testConfig.proxyCreds);
            response = (JsonObject) cs.login(new UsernamePasswordCredentials(username, newPassword), null);
            assertTrue(response.getInt("user_id") == userId);

        } catch (CardsavrRESTException e) {
            System.out.println(e.getRESTErrors()[0]);
            assert(false);
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            assert(false);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            assert(false);
        }
        session.delete("/cardsavr_users", userId, headers);
        session.delete("/integrators", integratorId, headers);
    }

    @Test
    public void createAndUpdateAccount() throws IOException, CardsavrRESTException, NoSuchAlgorithmException {

        CardsavrSession.APIHeaders headers = this.session.createHeaders();
        String random = RandomStringUtils.random(6, true, true);
        headers.safeKey = Base64.getEncoder().encodeToString(Encryption.sha256Hash(random.getBytes()));
        JsonObject response = null;
        try {
            String data = new String(Files.readAllBytes(Paths.get("./account_data.json")), StandardCharsets.UTF_8)
                .replaceAll("\\{\\{CARDHOLDER_UNIQUE_KEY\\}\\}", random);
            JsonObject jsonobj = Json.createReader(new StringReader(data)).read().asJsonObject();
            response = (JsonObject) session.post("/cardsavr_accounts", jsonobj, headers);
        } catch (CardsavrRESTException e) {
            System.out.println(e.getRESTErrors()[0]);
            assert(false); return;
        } catch (IOException e) {
            e.printStackTrace();
            assert(false); return;
        }
        int accountId = response.getInt("id");
        int cardholderId = response.getInt("cardholder_id");
        int merchantSiteId = response.getInt("merchant_site_id");
        assertTrue("Create account should return a valid id", accountId > 0);
        assertTrue("Create account should return a valid cardholder id", cardholderId > 0);

        try {
            JsonObject obj = Json.createObjectBuilder()
                .add("customer_key", response.getString("customer_key"))
                .add("username", "good_emaiil")
                .add("cardholder_id", cardholderId)
                .add("merchant_site_id", merchantSiteId)
                .add("password", "tfa").build();
            response = (JsonObject) session.put("/cardsavr_accounts", null, obj, headers);
        } catch (CardsavrRESTException e) {
            System.out.println(e.getRESTErrors()[0]);
            assert(false); return;
        } catch (IOException e) {
            e.printStackTrace();
            assert(false); return;
        }
        assertTrue("Object was not updated", !response.getString("last_updated_on").equals(response.getString("created_on")));
    }
    @Test
    public void jobPostJobTest() {
        runJobTest("JOB");
    }
    
    @Test
    public void jobPostCardholderMessageTest() {
        runJobTest("CARDHOLDER");
    }

    private JsonObject insertValue(JsonObject source, String key, String value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, value);
        source.entrySet().
                forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    private void runJobTest(String type)  {

        CardsavrSession.APIHeaders headers = this.session.createHeaders();
        JsonObject response = null;
        try {
            String data = new String(Files.readAllBytes(Paths.get("./job_data.json")), StandardCharsets.UTF_8)
                .replaceAll("\\{\\{CARDHOLDER_UNIQUE_KEY\\}\\}", RandomStringUtils.random(6, true, true));
            JsonObject jsonobj = Json.createReader(new StringReader(data)).read().asJsonObject();
            if (testConfig.queueNameOverride != null) {
                jsonobj = insertValue(jsonobj, "queue_name_override", testConfig.queueNameOverride);
            };
            response = (JsonObject) session.post("/place_card_on_single_site_jobs", jsonobj, headers);
            
        } catch (CardsavrRESTException e) {
            System.out.println(e.getRESTErrors()[0]);
            assert(false); return;
        } catch (IOException e) {
            e.printStackTrace();
            assert(false); return;
        }
        int jobId = response.getInt("id");
        int cardholderId = response.getInt("cardholder_id");
        assertTrue("Place job should return a valid id", jobId > 0);
        assertTrue("Place job should return a valid id", cardholderId > 0);

        final CountDownLatch latch = new CountDownLatch(1);

        List<String> assertStatuses = new LinkedList<>();
        Timer t = new Timer();
        int delay = 5000;
        t.scheduleAtFixedRate(new TimerTask() {

            private int totalTime = 0;

            public void run() {

                totalTime += delay;

                try {
                    String status = null;
                    if (type.equals("CARDHOLDER")) {
                        JsonArray messages = (JsonArray) session.get("/messages/cardholders/", cardholderId, null);
                        Iterator<JsonValue> iter = messages.iterator();
                        while (iter.hasNext()) {
                            JsonObject json = (JsonObject)iter.next();    
                            if (json.getString("type").equals("job_status")) {
                                JsonObject msg = json.getJsonObject("message");
                                status = msg.getString("status");
                                if (!assertStatuses.contains(status)) {
                                    assertStatuses.add(status);
                                }
                                System.out.println(status);
                                int jobId = json.getInt("job_id");
                                //if we see a pending message, grab that job and deal with credential requests.
                                if (handleCredentialRequest(session, jobId, null)) {
                                    //nothing to do here, credential request is handled.
                                } else if (msg.containsKey("termination_type")) {
                                    t.cancel();
                                    latch.countDown();
                                }
                            } // ignore type credential_request messaages
                        }
                    } else if (type.equals("JOB")) {
                        CardsavrSession.APIHeaders headers = session.createHeaders();
                        headers.hydration = Json.createArrayBuilder().add("credential_requests").build();
        
                        JsonObject job = (JsonObject) session.get("/place_card_on_single_site_jobs", jobId, headers);
                        status = job.getString("status");
                        System.out.println(status);
                        if (!assertStatuses.contains(status)) {
                            assertStatuses.add(status);
                        }
                        if (handleCredentialRequest(session, jobId, job)) {
                            //dealt with credential request
                        } else if (!job.get("completed_on").toString().equals("null")) {
                            t.cancel();
                            latch.countDown();
                        }
                    }
                    if (totalTime > 300000) {
                        throw new IOException("Task timed out after five minutes, exit.");
                    } else if (totalTime > 60000 && "QUEUED".equals(status)) {
                        throw new IOException("Task shouldn't be queued for more than one minute.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    latch.countDown();
                } catch (CardsavrRESTException e) {
                    System.out.println(e.getRESTErrors()[0]);
                    e.printStackTrace();
                    latch.countDown();
                } catch (JsonException e) {
                    e.printStackTrace();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                    latch.countDown();
                }      
            }
        }, 1000, delay); //wait one second, then wait five
        try {
            latch.await();
            assertEquals("Place job should finish with a status of SUCCESSFUL", "SUCCESSFUL", assertStatuses.get(assertStatuses.size() - 1));
            assertTrue("Status entries length should be at least 5", assertStatuses.size() >= 5);
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            assert(false);
        }
    }

    private boolean handleCredentialRequest(CardsavrSession session, int jobId, JsonObject job) throws IOException, CardsavrRESTException {
        
        if (job == null) {
            CardsavrSession.APIHeaders headers = session.createHeaders();
            headers.hydration = Json.createArrayBuilder().add("credential_requests").build();
            job = (JsonObject)session.get("/place_card_on_single_site_jobs/", jobId, headers);
        }
        
        JsonArray arr = (JsonArray)job.get("credential_requests");
        if (arr != null && arr.size() == 1 && arr.getJsonObject(0).getString("envelope_id") != null) {
            CardsavrSession.APIHeaders headers = session.createHeaders();
            headers.envelopeId = arr.getJsonObject(0).getString("envelope_id");
            JsonObject newCreds = null;
            String messageType = arr.getJsonObject(0).getString("type");
            if (messageType.equals("credential_request") || messageType.equals("initial_account_link")) {
                newCreds = Json.createObjectBuilder()
                    .add("account", Json.createObjectBuilder()
                        .add("account_link", Json.createObjectBuilder()
                            .add("username", "good_email")
                            .add("password", "tfa")
                            .build())
                        .build())
                    .build();
            } else if (messageType.startsWith("tfa")) {
                newCreds = Json.createObjectBuilder()
                    .add("account", Json.createObjectBuilder()
                        .add("account_link", Json.createObjectBuilder()
                            .add("tfa", "1234")
                            .build())
                        .build())
                    .build();
                }
            session.put("/place_card_on_single_site_jobs", jobId, newCreds, headers);
            return true;
        }
        return false;
    }

}
 