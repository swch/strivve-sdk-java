package com.strivve;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
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
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.io.Serializable;

public class CardsavrSession {

    private CardsavrClient client = new CardsavrClient();

    public static void rejectUnauthorized(boolean reject) { 
        
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { 
            new X509TrustManager() {     
                public X509Certificate[] getAcceptedIssuers() { 
                    return new X509Certificate[0];
                } 
                public void checkClientTrusted( 
                    java.security.cert.X509Certificate[] certs, String authType) {
                    } 
                public void checkServerTrusted( 
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
            } 
        }; 
        
        if (!reject) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL"); 
                sc.init(null, trustAllCerts, new java.security.SecureRandom()); 
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (GeneralSecurityException e) {
            }         
        }
    }

    public static CardsavrSession createSession(String integratorName, String integratorKey, URL apiServer)
            throws IOException {
        return createSession(integratorName, integratorKey, apiServer, null, -1, null, null);
    }

    public static CardsavrSession createSession(String integratorName, String integratorKey, URL apiServer, String proxyhost, int proxyport, String username, String password)
            throws IOException {
        return new CardsavrSession(integratorName, integratorKey, apiServer, proxyhost, proxyport, username, password);
    }

    private transient String integratorName;
    private transient SecretKey integratorKey;
    private transient Proxy proxy;
    private transient URL apiServer;
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

    private CardsavrSession(String integratorName, String integratorKey, URL apiServer, String proxyhost, int proxyport, String username, String password)
            throws IOException {

        this.integratorName = integratorName;
        this.integratorKey = Encryption.convertRawToAESKey(Base64.getDecoder().decode(integratorKey));
        this.apiServer = apiServer;
        if (proxyhost != null) {
            Authenticator authenticator = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(username,
                            password.toCharArray()));
                }
            };
            Authenticator.setDefault(authenticator);
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyhost, proxyport));

        }
    }

    public void restore(byte[] sessionObjects) throws ClassNotFoundException, IOException {
        this.sessionObjects = (SessionObjects)(new ObjectInputStream(new ByteArrayInputStream(sessionObjects))).readObject();
    }

    public JsonObject login(String username, String password, JsonObject trace) throws IOException,
            CardsavrRESTException {
        
        // Trace key can be overridden, but almost always starts with the application username
        this.sessionObjects.sessionTrace = trace != null ? trace : Json.createObjectBuilder().add("key", integratorName).build();

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
                .add("username", username)
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

    public JsonValue get(String path, Map<String, String> filters, APIHeaders headers) throws IOException,
            CardsavrRESTException {
        return client.apiRequest(this.makeConnection("GET", path, filters, this.proxy), null, headers);
    }

    public JsonValue get(String path, int id, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(this.makeConnection("GET", Paths.get(path, Integer.toString(id)).toString(), null, this.proxy), null, headers);
    }

    public JsonValue post(String path, JsonObject body, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(this.makeConnection("POST", path, null, this.proxy), body, headers);
    }

    public JsonValue put(String path, Map<String, String>  filters, JsonObject body, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(this.makeConnection("PUT", path, filters, this.proxy), body, headers);
    }

    public JsonValue put(String path, int id, JsonObject body, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(this.makeConnection("PUT", Paths.get(path, Integer.toString(id)).toString(), null, this.proxy), body, headers);
    }

    public JsonValue delete(String path, int id, APIHeaders headers) throws IOException, CardsavrRESTException {
        return client.apiRequest(this.makeConnection("DELETE", Paths.get(path, Integer.toString(id)).toString(), null, this.proxy), null, headers);
    }

    private HttpsURLConnection makeConnection(String method, String path, Map<String, String> filters, Proxy proxy) throws MalformedURLException, UnsupportedEncodingException, IOException {
        HttpsURLConnection conn = (proxy != null) ? 
            ((HttpsURLConnection) makeURL(path, filters).openConnection(proxy)) :
            ((HttpsURLConnection) makeURL(path, filters).openConnection());
        conn.setRequestMethod(method);
        return conn;
    }

    private URL makeURL(String path, Map<String, String> filters) throws MalformedURLException, UnsupportedEncodingException {
        path = path.replace('\\', '/');
        if (filters != null) {
            path += "?" + filters.entrySet().stream()
                .map(p -> encodeUTF8(p.getKey()) + "=" + encodeUTF8(p.getValue()))
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");
        }
        return new URL("https", apiServer.getHost(), apiServer.getPort(), path);
    }

    private static String encodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //dumb
        }
        return s;
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

        private void populateHeaders(HttpURLConnection request)
                throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
            if (hydration != null)
                request.setRequestProperty("x-cardsavr-hydration", hydration.toString());
            if (paging != null)
                request.setRequestProperty("x-cardsavr-paging", paging.toString());
            if (trace != null)
                request.setRequestProperty("x-cardsavr-trace", trace.toString());
            if (safeKey != null)
                request.setRequestProperty("x-cardsavr-cardholder-safe-key", Encryption.encryptAES256(safeKey, sessionObjects.sessionKey));
            if (newSafeKey != null)
                request.setRequestProperty("x-cardsavr-new-cardholder-safe-key", Encryption.encryptAES256(newSafeKey, sessionObjects.sessionKey));
            if (financialInsitution != null)
                request.setRequestProperty("x-cardsavr-financial-institution", financialInsitution);
            if (envelopeId != null)
                request.setRequestProperty("x-cardsavr-envelope-id", envelopeId);
        }
    }

    private final class CardsavrClient {

        private JsonValue apiRequest(HttpURLConnection connection, JsonObject jsonBody, APIHeaders headers)
                throws IOException, CardsavrRESTException {

             try {
                SecretKey encryptionKey = sessionObjects.sessionKey != null ? sessionObjects.sessionKey : integratorKey;
                String fullBody = null;

                if (jsonBody != null) {
                    final String[] methods = {"PATCH", "POST", "PUT"};
                    if (Arrays.binarySearch(methods, connection.getRequestMethod().toUpperCase()) < 0) {
                        throw new CardsavrRESTException("GET & DELETE must have null jsonBody "  + connection.getURL().getPath(), null, null);
                    }
                    String encryptedBody = Encryption.encryptAES256(jsonBody.toString(), encryptionKey);
                    fullBody = Json.createObjectBuilder()
                        .add("encrypted_body", encryptedBody)
                        .build()
                        .toString();
                    connection.setRequestProperty("Content-type", "application/json");
                } 
    
                if (sessionObjects.sessionToken != null) {
                    connection.setRequestProperty("x-cardsavr-session-jwt", sessionObjects.sessionToken);
                }

                String authorization = "SWCH-HMAC-SHA256 Credentials=" + integratorName;
                String nonce = Long.toString(new Date().getTime());
                String signature = Signing.signRequest(connection.getURL().getPath(), authorization, nonce, encryptionKey, fullBody);

                String version = getClass().getPackage().getImplementationVersion();
                connection.setRequestProperty("x-cardsavr-client-application", integratorName + " Strivve Java SDK v" + version);
                connection.setRequestProperty("x-cardsavr-trace", sessionObjects.sessionTrace.toString());
                connection.setRequestProperty("x-cardsavr-nonce", nonce);
                connection.setRequestProperty("x-cardsavr-authorization", authorization);
                connection.setRequestProperty("x-cardsavr-signature", signature);
    
                if (headers != null) {
                    headers.populateHeaders(connection);
                }
                if (fullBody != null) {
                    connection.setDoOutput(true);
                    OutputStream os = connection.getOutputStream();
                    os.write(fullBody.getBytes());
                    os.flush();
                    os.close();
                }
                    
                int responseCode = connection.getResponseCode();

                if (responseCode == 404) {
                    throw new FileNotFoundException(responseCode + " Couldn't locate entity for: " + connection.getURL().getPath());
                } else if (responseCode == 429) {
                    throw new IOException(responseCode + " Too many requests for: " + connection.getURL().getPath());
                } else if (responseCode == 403 || responseCode == 401) {
                    throw new SecurityException(responseCode + " " + connection.getURL().getPath());
                }

                try {
                    authorization = connection.getHeaderField("x-cardsavr-authorization");
                    nonce = connection.getHeaderField("x-cardsavr-nonce");
                    signature = connection.getHeaderField("x-cardsavr-signature");
                } catch (NullPointerException e) {
                    throw new CardsavrRESTException(
                        "Missing signature/nonce/authorization header - error calling " + connection.getURL().getPath(), 
                        null, null);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
                String result = br.lines().collect(Collectors.joining());

                if (result.length() > 0) {
                    String body;
                    fullBody = null;
                    try (JsonReader reader = Json.createReader(new StringReader(result))) {
                        JsonStructure jsonst = reader.read();
                        fullBody = jsonst.toString();
                        String encryptedBody = jsonst.asJsonObject().getString("encrypted_body");
                        body = Encryption.decryptAES256(encryptedBody, encryptionKey);
                    }

                    try {
                        Signing.verifySignature(signature, connection.getURL().getPath(), authorization, nonce, new SecretKey[] {encryptionKey}, fullBody);
                    } catch (SignatureException e) {
                        throw new CardsavrRESTException(
                            "Invalid signature header: " + signature + " - error calling " + connection.getURL().getPath(), 
                            null, null);
                    }

                    try (JsonReader reader = Json.createReader(new StringReader(body))) {
                        JsonStructure jsonst = reader.read();
                        if (jsonst.getValueType() == ValueType.ARRAY) {
                            return jsonst.asJsonArray();
                        } else {
                            JsonObject obj = jsonst.asJsonObject();
                            if (obj.getJsonArray("_errors") != null) {
                                throw new CardsavrRESTException(
                                    responseCode + " - error calling " + connection.getURL().getPath(), 
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