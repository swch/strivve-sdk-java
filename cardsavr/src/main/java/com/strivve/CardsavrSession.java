package com.strivve;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

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
import java.io.Serializable;

public class CardsavrSession {

    private CardsavrClient client = new CardsavrClient();

    private static boolean rejectUnauthorized = true;
    public static void rejectUnauthorized(boolean reject) { rejectUnauthorized = reject; }

    public static CardsavrSession createSession(String integratorName, String integratorKey, HttpHost apiServer)
            throws IOException {
        return createSession(integratorName, integratorKey, apiServer, null, null);
    }

    public static CardsavrSession createSession(String integratorName, String integratorKey, HttpHost apiServer, HttpHost proxyhost, UsernamePasswordCredentials creds)
            throws IOException {
        return new CardsavrSession(integratorName, integratorKey, apiServer, proxyhost, creds);
    }

    private transient String integratorName;
    private transient SecretKey integratorKey;
    transient CloseableHttpClient httpClient;
    private transient HttpHost apiServer;
    private SessionObjects sessionObjects = new SessionObjects();

    private final class SessionObjects implements Serializable {
        private JsonObject sessionTrace;
        private SecretKey sessionKey;
        private String sessionToken;

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.writeObject(sessionTrace.toString());
            out.writeObject(sessionKey);
            out.writeObject(sessionToken);
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            sessionTrace = Json.createReader(new StringReader((String)in.readObject())).readObject();
            sessionKey = (SecretKey)in.readObject();
            sessionToken = (String)in.readObject();
        }
    }

    public byte[] serializeSessionObjects() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(sessionObjects);
        return baos.toByteArray();
    }

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

    public void restore(byte[] sessionObjects) throws ClassNotFoundException, IOException {
        this.sessionObjects = (SessionObjects)(new ObjectInputStream(new ByteArrayInputStream(sessionObjects))).readObject();
    }

    public JsonObject login(UsernamePasswordCredentials cardsavrCreds, JsonObject trace) throws IOException,
            CardsavrRESTException {
        
        // Trace key can be overridden, but almost always starts with the application username
        this.sessionObjects.sessionTrace = trace != null ? trace : Json.createObjectBuilder().add("key", integratorName).build();

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
                .add("username", cardsavrCreds.getUserName())
                .add("password_proof", passwordProof)
                .add("client_public_key", clientPublicKeyBase64)
                .build();
            JsonObject loginResponse = (JsonObject)post("/session/login", body, null);
            
            this.sessionObjects.sessionToken = loginResponse.getString("session_token");
            String base64ServerPublicKey = loginResponse.getString("server_public_key");
            ECPublicKey serverPublicKey = UncompressedPublicKeys.decodeUncompressedECPublicKey(
                    ((ECPublicKey) kp.getPublic()).getParams(), Base64.getDecoder().decode(base64ServerPublicKey));

            this.sessionObjects.sessionKey = Encryption.generateECSecretKey(serverPublicKey, kp.getPrivate());

            return loginResponse;
        } catch (IOException e) {
            throw e;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchProviderException
                | InvalidAlgorithmParameterException e) {
            throw new CardsavrEncryptionException(e.getMessage(), e);
        }
    }

    public JsonObject end() throws IOException, CardsavrRESTException {
        return (JsonObject)get("/session/end", null, null);
    }

    public JsonValue get(String path, List<NameValuePair> filters, APIHeaders headers) throws IOException,
            CardsavrRESTException {
        return client.apiRequest(new HttpGet(makeURLString(path, filters)), null, headers);
    }

    public JsonValue get(String path, int id, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(new HttpGet(makeURLString(Paths.get(path, Integer.toString(id)).toString(), null)), null, headers);
    }

    public JsonValue post(String path, JsonObject body, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(new HttpPost(makeURLString(path, null)), body, headers);
    }

    public JsonValue put(String path, List<NameValuePair> filters, JsonObject body, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(new HttpPut(makeURLString(path, filters)), body, headers);
    }

    public JsonValue put(String path, int id, JsonObject body, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(new HttpPut(makeURLString(Paths.get(path, Integer.toString(id)).toString(), null)), body, headers);
    }

    public JsonValue delete(String path, int id, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(new HttpDelete(makeURLString(Paths.get(path, Integer.toString(id)).toString(), null)), null, headers);
    }

    private String makeURLString(String path, List<NameValuePair> filters) throws MalformedURLException {
        path = path.replace('\\', '/');
        if (filters != null) {
            path += "?" + URLEncodedUtils.format(filters, StandardCharsets.UTF_8);
        }
        return new URL("https", apiServer.getHostName(), apiServer.getPort(), path).toString();
    }

    public APIHeaders createHeaders() {
        return new APIHeaders();
    }
    
    public final class APIHeaders {
        public JsonArray hydration;
        public JsonObject paging;
        public JsonObject trace;
        public String safeKey;
        public String newSafeKey;
        public String financialInsitution;
        public String envelopeId;

        public JsonArray createJsonArray(String[] values) {
            JsonArrayBuilder builder = Json.createArrayBuilder();
            for (String value : values) {
                builder.add(value);
            }
            return builder.build();        
        }

        private void populateHeaders(HttpUriRequest request)
                throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
            if (hydration != null)
                request.setHeader("x-cardsavr-hydration", hydration.toString());
            if (paging != null)
                request.setHeader("x-cardsavr-paging", paging.toString());
            if (trace != null)
                request.setHeader("x-cardsavr-trace", trace.toString());
            if (safeKey != null)
                request.setHeader("x-cardsavr-cardholder-safe-key", Encryption.encryptAES256(safeKey, sessionObjects.sessionKey));
            if (newSafeKey != null)
                request.setHeader("x-cardsavr-new-cardholder-safe-key", Encryption.encryptAES256(newSafeKey, sessionObjects.sessionKey));
            if (financialInsitution != null)
                request.setHeader("x-cardsavr-financial-institution", financialInsitution);
            if (envelopeId != null)
                request.setHeader("x-cardsavr-envelope-id", envelopeId);
        }
    }

    private final class CardsavrClient {

        private JsonValue apiRequest(HttpUriRequest request, JsonObject jsonBody, APIHeaders headers)
                throws IOException, CardsavrRESTException {

            String authorization = "SWCH-HMAC-SHA256 Credentials=" + integratorName;
            String nonce = Long.toString(new Date().getTime());
            String requestSigning = request.getURI().toURL().getFile() + authorization + nonce;

             try {
                SecretKey encryptionKey = sessionObjects.sessionKey != null ? sessionObjects.sessionKey : integratorKey;

                if (jsonBody != null && (request instanceof HttpEntityEnclosingRequestBase)) {
                    String encryptedBody = Encryption.encryptAES256(jsonBody.toString(), encryptionKey);
                    String newBody = Json.createObjectBuilder()
                        .add("encrypted_body", encryptedBody)
                        .build()
                        .toString();
                    ((HttpEntityEnclosingRequestBase)request).setEntity(new StringEntity(newBody));
                    requestSigning += newBody;
                    request.setHeader("Content-type", "application/json");
                } 
    
                if (sessionObjects.sessionToken != null) {
                    request.setHeader("x-cardsavr-session-jwt", sessionObjects.sessionToken);
                }
                String signature = Encryption.hmacSign(requestSigning.getBytes(), encryptionKey.getEncoded());                
                String version = getClass().getPackage().getImplementationVersion();
                request.setHeader("x-cardsavr-client-application", integratorName + " Strivve Java SDK v" + version);
                request.setHeader("x-cardsavr-trace", sessionObjects.sessionTrace.toString());
                request.setHeader("x-cardsavr-nonce", nonce);
                request.setHeader("x-cardsavr-authorization", authorization);
                request.setHeader("x-cardsavr-signature", signature);
    
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
                if (result.length() > 0) {
                    String body;
                    try (JsonReader reader = Json.createReader(new StringReader(result))) {
                        JsonStructure jsonst = reader.read();
                        JsonObject jsonobj =jsonst.asJsonObject();
                        String encryptedBody = jsonobj.getString("encrypted_body");
                        body = Encryption.decryptAES256(encryptedBody, encryptionKey);
                    }
    
                    try (JsonReader reader = Json.createReader(new StringReader(body))) {
                        JsonStructure jsonst = reader.read();
                        if (jsonst.getValueType() == ValueType.ARRAY) {
                            return jsonst.asJsonArray();
                        } else {
                            JsonObject obj = jsonst.asJsonObject();
                            if (obj.getJsonArray("_errors") != null) {
                                throw new CardsavrRESTException(
                                    response.getStatusLine() + " - error calling " + request.getURI().getPath(), 
                                    obj.getJsonArray("_errors"), obj);
                            }
                            return obj;
                        } 
                    }
                } else {
                    return JsonValue.EMPTY_JSON_OBJECT;
                }       
            } catch (IOException e) {
                throw e;
            } catch (InvalidKeyException | NoSuchAlgorithmException | 
                     NoSuchPaddingException | InvalidAlgorithmParameterException | 
                     IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
                throw new CardsavrEncryptionException(e.getMessage(), e);
            }
        }
    }
}