package com.strivve;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
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
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
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
    public static void rejectUnauthorized(boolean reject) { rejectUnauthorized = reject; }

    private static String chopPrefix(String key) {
        //return key;
        return key.replace("x-cardsavr-", "");
    }

    private static String makeCamelCase(String key) {
//        return key;
        return StringUtils.uncapitalize(Arrays.asList(key.split("_"))
            .stream()
            .reduce("", (acc, element) -> acc + StringUtils.capitalize(element)))
            .replace("username", "userName");
    }

    public static CardsavrSession createSession(String integratorName, String integratorKey, HttpHost apiServer)
            throws IOException {
        return createSession(integratorName, integratorKey, apiServer, null, null);
    }

    public static CardsavrSession createSession(String integratorName, String integratorKey, HttpHost apiServer, HttpHost proxyhost, UsernamePasswordCredentials creds)
            throws IOException {
        return new CardsavrSession(integratorName, integratorKey, apiServer, proxyhost, creds);
    }

    private String integratorName;
    private SecretKey integratorKey;
    private SecretKey sessionKey;
    private JsonObject sessionTrace;
    private String sessionToken;
    CloseableHttpClient httpClient;
    private HttpHost apiServer;

    private CardsavrSession(String integratorName, String integratorKey, HttpHost apiServer, HttpHost proxyhost, UsernamePasswordCredentials proxyCreds)
            throws IOException {

        this.integratorName = integratorName;
        this.integratorKey = Encryption.convertRawToAESKey(Base64.getDecoder().decode(integratorKey));
        this.apiServer = apiServer;
        this.httpClient = buildHttpClient(proxyhost, proxyCreds);
    }

    private CloseableHttpClient buildHttpClient(HttpHost proxyhost, UsernamePasswordCredentials creds) throws IOException {

        HttpClientBuilder builder = HttpClients.custom();
        //can't run rejectunauthorized through a proxy...sorry!
        if (!CardsavrSession.rejectUnauthorized) {
            try {
                builder = builder
                    .setSSLContext(new SSLContextBuilder()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
                throw new IOException("Unable to establish insecure HttpClient: " + e.getMessage());
            }
        }
        if (proxyhost != null) {
            if (creds != null) {
                CredentialsProvider credentialsPovider = new BasicCredentialsProvider();
                credentialsPovider.setCredentials(new AuthScope(proxyhost.getHostName(), proxyhost.getPort()), creds);
                builder = builder.setDefaultCredentialsProvider(credentialsPovider);
            }
            builder = builder.setRoutePlanner(new DefaultProxyRoutePlanner(proxyhost));
        } else if (creds != null) {
            throw new IOException("Cannot set credentials on non-proxy connection.");            
        }
        return builder.build();
    }

    public JsonObject login(UsernamePasswordCredentials cardsavrCreds, JsonObject trace) throws IOException,
            CarsavrRESTException {
        
        // Trace key can be overridden, but almost always starts with the application username
        this.sessionTrace = trace != null ? trace : Json.createObjectBuilder().add("key", integratorName).build();

        try {
            // generate a passwordProof based on the integratorKey
            String passwordProof = Encryption.generateSignedPasswordKey(cardsavrCreds.getPassword(), cardsavrCreds.getUserName(), integratorKey.getEncoded());

            // generate a key pair for the client, share the public key with the server
            KeyPair kp;
            kp = Encryption.generateECKeys();
            // export the raw public key as base64, not trivial in Java, so we use the
            // UncompressedPublicKeys lib
            String clientPublicKeyBase64 = Base64.getEncoder()
                    .encodeToString(UncompressedPublicKeys.encodeUncompressedECPublicKey((ECPublicKey) kp.getPublic()));

            JsonObject body = Json.createObjectBuilder()
                .add(makeCamelCase("username"), cardsavrCreds.getUserName())
                .add(makeCamelCase("password_proof"), passwordProof)
                .add(makeCamelCase("client_public_key"), clientPublicKeyBase64)
                .build();
            JsonObject loginResponse = (JsonObject)post("/session/login", body, null);
            
            this.sessionToken = loginResponse.getString(makeCamelCase("session_token"));
            String base64ServerPublicKey = loginResponse.getString(makeCamelCase("server_public_key"));
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
        return new URL("https", apiServer.getHostName(), apiServer.getPort(), path).toString();
    }

    public APIHeaders createHeaders() {
        return new APIHeaders();
    }
    
    public final class APIHeaders {
        protected JsonArray hydration;
        protected JsonObject paging;
        protected JsonObject trace;
        protected String safeKey;
        protected String newSafeKey;
        protected String financialInsitution;
		protected String envelopeId;

        private void populateHeaders(HttpUriRequest request)
                throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
            if (hydration != null)
                request.setHeader(chopPrefix("x-cardsavr-hydration"), hydration.toString());
            if (paging != null)
                request.setHeader(chopPrefix("x-cardsavr-paging"), paging.toString());
            if (trace != null)
                request.setHeader(chopPrefix("x-cardsavr-trace"), trace.toString());
            if (safeKey != null)
                request.setHeader(chopPrefix("x-cardsavr-cardholder-safe-key"), Encryption.encryptAES256(safeKey, sessionKey));
            if (newSafeKey != null)
                request.setHeader(chopPrefix("x-cardsavr-new-cardholder-safe-key"), Encryption.encryptAES256(newSafeKey, sessionKey));
            if (financialInsitution != null)
                request.setHeader(chopPrefix("x-cardsavr-financial-institution"), financialInsitution);
            if (envelopeId != null)
                request.setHeader(chopPrefix("x-cardsavr-envelope-id"), envelopeId);
        }
    }

    private final class CardsavrClient {

        private JsonValue apiRequest(HttpUriRequest request, JsonObject jsonBody, APIHeaders headers)
                throws IOException, CarsavrRESTException {

            String authorization = "SWCH-HMAC-SHA256 Credentials=" + integratorName;
            String nonce = Long.toString(new Date().getTime());
            String requestSigning = request.getURI().toURL().getFile() + authorization + nonce;

             try {
                SecretKey encryptionKey = sessionKey != null ? sessionKey : integratorKey;

                if (jsonBody != null && (request instanceof HttpEntityEnclosingRequestBase)) {
                    String encryptedBody = Encryption.encryptAES256(jsonBody.toString(), encryptionKey);
                    String newBody = Json.createObjectBuilder()
                        .add(makeCamelCase("encrypted_body"), encryptedBody)
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
                request.setHeader(chopPrefix("x-cardsavr-client-application"), integratorName);
                request.setHeader(chopPrefix("x-cardsavr-trace"), sessionTrace.toString());
                request.setHeader(chopPrefix("x-cardsavr-nonce"), nonce);
                request.setHeader(chopPrefix("x-cardsavr-authorization"), authorization);
                request.setHeader(chopPrefix("x-cardsavr-signature"), signature);
    
                if (headers != null) {
                    headers.populateHeaders(request);
                }
        
                HttpResponse response = httpClient.execute(request);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    throw new FileNotFoundException(response.getStatusLine() + " Couldn't locate entity for: " + request.getURI().toURL().getFile());
                } else if (response.getStatusLine().getStatusCode() == 429) {
                    throw new IOException(response.getStatusLine() + " Too many requests for: " + request.getURI().toURL().getFile());
                } else if (response.getStatusLine().getStatusCode() == 403 || response.getStatusLine().getStatusCode() == 401) {
                    throw new SecurityException(response.getStatusLine() + " " + request.getURI().toURL().getFile());
                }
                String result = EntityUtils.toString(response.getEntity());
        
                String body;
                try (JsonReader reader = Json.createReader(new StringReader(result))) {
                    JsonStructure jsonst = reader.read();
                    JsonObject jsonobj =jsonst.asJsonObject();
                    String encryptedBody = jsonobj.getString(makeCamelCase("encrypted_body"));
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
                e.printStackTrace();
                throw new CarsavrEncryptionException(e.getMessage(), e);
            }
        }
    }
}