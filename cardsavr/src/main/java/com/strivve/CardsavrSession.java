package com.strivve;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;

public class CardsavrSession {

    private CardsavrClient client = new CardsavrClient();

    private static boolean rejectUnauthorized = true;
    public static void rejectUnauthroized(boolean reject) { rejectUnauthorized = reject; }

    public static CardsavrSession createSession(String integratorName, String integratorKey, String apiInstance) {
        return new CardsavrSession(integratorName, integratorKey, apiInstance);
    }

    private String integratorName;
    private SecretKey integratorKey;
    private String apiInstance;
    private SecretKey sessionKey;
    private JsonObject sessionTrace;
    private String sessionToken;

    private CardsavrSession(String integratorName, String integratorKey, String apiInstance) {

        this.integratorName = integratorName;
        this.integratorKey = Encryption.convertRawToAESKey(Base64.getDecoder().decode(integratorKey));
        this.apiInstance = apiInstance;
        this.sessionTrace = Json.createObjectBuilder().add("key", integratorKey).build();
    }

    public JsonObject login(String username, String password, JsonObject trace) throws IOException,
            CarsavrRESTException {
        
        if (trace != null) 
            this.sessionTrace = trace;
        
        try {
            // generate a passwordProof based on the integratorKey
            String passwordProof = Encryption.generateSignedPasswordKey(password, username, integratorKey.getEncoded());

            // generate a key pair for the client, share the public key with the server
            KeyPair kp;
            kp = Encryption.generateECKeys();
            // export the raw public key as base64, not trivial in Java, so we use the
            // UncompressedPublicKeys lib
            String clientPublicKeyBase64 = Base64.getEncoder()
                    .encodeToString(UncompressedPublicKeys.encodeUncompressedECPublicKey((ECPublicKey) kp.getPublic()));

            JsonObject body = Json.createObjectBuilder()
                .add("userName", username)
                .add("passwordProof", passwordProof)
                .add("clientPublicKey", clientPublicKeyBase64)
                .build();

            JsonObject loginResponse = (JsonObject)post("/session/login", body, null);
            
            this.sessionToken = loginResponse.getString("sessionToken");
            String base64ServerPublicKey = loginResponse.getString("serverPublicKey");
            ECPublicKey serverPublicKey = UncompressedPublicKeys.decodeUncompressedECPublicKey(
                    ((ECPublicKey) kp.getPublic()).getParams(), Base64.getDecoder().decode(base64ServerPublicKey));

            this.sessionKey = Encryption.generateECSecretKey(serverPublicKey, kp.getPrivate());

            return loginResponse;
        } catch (IOException e) {
            throw e;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchProviderException
                | InvalidAlgorithmParameterException e) {
            throw new CarsavrEncryptionException(e.getMessage(), e);
        }
    }

    public JsonObject end() throws IOException, CarsavrRESTException {
        return (JsonObject)get("/session/end", null, null);
    }

    public JsonValue get(String path, List<NameValuePair> filters, APIHeaders headers) throws IOException,
            CarsavrRESTException {
        return client.apiRequest(new HttpGet(makeURLString(path, filters)), null, headers);
    }

    public JsonValue get(String path, int id, APIHeaders headers) throws IOException, CarsavrRESTException {
        return client.apiRequest(new HttpGet(makeURLString(Paths.get(path, Integer.toString(id)).toString(), null)), null, headers);
    }

    public JsonValue post(String path, JsonObject body, APIHeaders headers) throws IOException, CarsavrRESTException {
        return client.apiRequest(new HttpPost(makeURLString(path, null)), body, headers);
    }

    public JsonValue put(String path, List<NameValuePair> filters, JsonObject body, APIHeaders headers) throws IOException, CarsavrRESTException {
        return client.apiRequest(new HttpPut(makeURLString(path, filters)), body, headers);
    }

    public JsonValue put(String path, int id, JsonObject body, APIHeaders headers) throws IOException, CarsavrRESTException {
        return client.apiRequest(new HttpPut(makeURLString(Paths.get(path, Integer.toString(id)).toString(), null)), body, headers);
    }

    public JsonValue delete(String path, int id, APIHeaders headers) throws IOException, CarsavrRESTException {
        return client.apiRequest(new HttpDelete(makeURLString(Paths.get(path, Integer.toString(id)).toString(), null)), null, headers);
    }

    private String makeURLString(String path, List<NameValuePair> filters) throws MalformedURLException {
        if (filters != null) {
            path += "?" + URLEncodedUtils.format(filters, StandardCharsets.UTF_8);
        }
        return new URL("https", apiInstance, 8443, path).toString();
    }

    public APIHeaders createHeaders() {
        return new APIHeaders();
    }
    
    public final class APIHeaders {
        JsonArray hydration;
        JsonObject paging;
        JsonObject trace;
        String safeKey;
        String newSafeKey;
        String financialInsitution;

        private void populateHeaders(HttpUriRequest request)
                throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
            if (hydration != null)
                request.setHeader("hydration", hydration.toString());
            if (paging != null)
                request.setHeader("paging", paging.toString());
            if (trace != null)
                request.setHeader("trace", trace.toString());
            if (safeKey != null)
                request.setHeader("cardholder-safe-key", Encryption.encryptAES256(safeKey, sessionKey));
            if (newSafeKey != null)
                request.setHeader("new-cardholder-safe-key", Encryption.encryptAES256(newSafeKey, sessionKey));
            if (financialInsitution != null)
                request.setHeader("financial-institution", financialInsitution);
        }
    }

    private final class CardsavrClient {

        private CloseableHttpClient createInsecureHttpClient() throws IOException {
            try {
                return HttpClients.custom()
                        .setSSLContext(new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build())
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
            } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
                throw new IOException("Unable to establish insecure HttpClient: " + e.getMessage());
            }
        }
        
        private JsonValue apiRequest(HttpUriRequest request, JsonObject jsonBody, APIHeaders headers)
                throws IOException, CarsavrRESTException {

            String authorization = "SWCH-HMAC-SHA256 Credentials=" + integratorName;
            String nonce = Long.toString(new Date().getTime());
            String requestSigning = request.getURI().toURL().getFile() + authorization + nonce;

            CloseableHttpClient httpclient = 
                CardsavrSession.rejectUnauthorized ? 
                HttpClients.createDefault() : 
                createInsecureHttpClient();
            try {
                SecretKey encryptionKey = sessionKey != null ? sessionKey : integratorKey;

                if (jsonBody != null && (request instanceof HttpEntityEnclosingRequestBase)) {
                    String encryptedBody = Encryption.encryptAES256(jsonBody.toString(), encryptionKey);
                    String newBody = Json.createObjectBuilder()
                        .add("encryptedBody", encryptedBody)
                        .build()
                        .toString();
                    ((HttpEntityEnclosingRequestBase)request).setEntity(new StringEntity(newBody));
                    requestSigning += newBody;
                    request.setHeader("Content-type", "application/json");
                } 
    
                if (sessionToken != null) {
                    request.setHeader("x-cardsavr-session-jwt", sessionToken);
                }
                String signature = Encryption.hmacSign(requestSigning.getBytes(), encryptionKey.getEncoded());                
                request.setHeader("client-application", integratorName);
                request.setHeader("trace", sessionTrace.toString());
                request.setHeader("nonce", nonce);
                request.setHeader("authorization", authorization);
                request.setHeader("signature", signature);
    
                if (headers != null) {
                    headers.populateHeaders(request);
                }
        
                HttpResponse response = httpclient.execute(request);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    throw new FileNotFoundException(response.getStatusLine() + " Couldn't locate entity for: " + request.getURI().toURL().getFile());
                }
                String result = EntityUtils.toString(response.getEntity());
        
                String body;
                try (JsonReader reader = Json.createReader(new StringReader(result))) {
                    JsonStructure jsonst = reader.read();
                    JsonObject jsonobj =jsonst.asJsonObject();
                    String encryptedBody = jsonobj.getString("encryptedBody");
                    body = Encryption.decryptAES256(encryptedBody, encryptionKey);
                }

                try (JsonReader reader = Json.createReader(new StringReader(body))) {
                    JsonStructure jsonst = reader.read();
                    if (jsonst.getValueType() == ValueType.ARRAY) {
                        return jsonst.asJsonArray();
                    } else {
                        JsonObject obj = jsonst.asJsonObject();
                        if (obj.getJsonArray("_errors") != null) {
                            throw new CarsavrRESTException(
                                response.getStatusLine() + " - error calling " + request.getURI().getPath(), 
                                obj.getJsonArray("_errors"), obj);
                        }
                        return obj;
                    } 
                }
            } catch (IOException e) {
                throw e;
            } catch (InvalidKeyException | NoSuchAlgorithmException | 
                     NoSuchPaddingException | InvalidAlgorithmParameterException | 
                     IllegalBlockSizeException | BadPaddingException e) {
                throw new CarsavrEncryptionException(e.getMessage(), e);
            } finally {
                httpclient.close();
            }
        }
    }
}