package com.strivve;

import static org.junit.Assert.*;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.Test;

/**
 * Unit test for simple App.
 */

public class SigningTest {
    public final static String nonce = "1695884151469";
    public final static String authorization = "SWCH-HMAC-SHA256 Credentials=cardupdatr_ux_integrator";
    public final static String path = "https://synthetic-sites-server.vercel.app/webhook";
    public final static String signature = "IvP5sku4yDw9ALA3hkaqodHNNCmPqYEYzfGtwY2ZdNE=";

    /*
    {
        'x-cardsavr-authorization': 'SWCH-HMAC-SHA256 Credentials=pscu-testing',
        'x-cardsavr-nonce': '1695682031755',
        'x-cardsavr-signature': 'Dd20upWAlKq2N+Cllt/3GjK81av0O/uLES2HsA8+g0M='
    */

    public final static String body = 
        "{\"jobs\":[{\"job_id\":228,\"completed_on\":\"2023-09-28T06:53:22.505Z\",\"merchant_site\":{\"id\":1,\"host\":\"synthetic-sites-server.vercel.app/index.html\",\"name\":\"SS Standard Login\"},\"termination_type\":\"BILLABLE\",\"job_status_message\":\"Your card was placed successfully.\",\"job_status\":\"SUCCESSFUL\",\"custom_data\":null,\"card_customer_key\":\"C111jwNWwu0YW6f4qN4UDBExKnNs=\"}],\"financial_institution_id\":2,\"financial_institution\":\"Default\",\"trace\":{\"key\":\"iiodknpfk33i57zmghij\"},\"custom_data\":null,\"cuid\":\"iiodknpfk33i57zmghij\",\"username\":\"iiodknpfk33i57zmghij\"}";
    //public final static String username = "username";
    //public final static String password = "password";
        
    public final static SecretKey whIntegratorKey = Encryption
        .convertRawToAESKey(Base64.getDecoder().decode("Gp0nCuj+XiA0pb8AFKUzqbx5JL1GzBexbzds4oqahAc="));
    public final static SecretKey[] keys = new SecretKey[1];

        @Test
        public void webhookHMACSignVerification() throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
            keys[0] = whIntegratorKey;
            //body.replaceAll(" ", "");
            boolean result = Signing.verifySignature(signature, path, authorization, nonce, keys, body);
            System.out.println("result\t "+result);
            assertTrue(result);
            
        }        
}

/* 
["WEBHOOK_DEBUG",
"cardupdatr_ux_integrator",
"Gp0nCuj+XiA0pb8AFKUzqbx5JL1GzBexbzds4oqahAc=",
"https://synthetic-sites-server.vercel.app/webhook",
"{\"jobs\":[{\"job_id\":228,\"completed_on\":\"2023-09-28T06:53:22.505Z\",\"merchant_site\":{\"id\":1,\"host\":\"synthetic-sites-server.vercel.app/index.html\",\"name\":\"SS Standard Login\"},\"termination_type\":\"BILLABLE\",\"job_status_message\":\"Your card was placed successfully.\",\"job_status\":\"SUCCESSFUL\",\"custom_data\":null,\"card_customer_key\":\"C111jwNWwu0YW6f4qN4UDBExKnNs=\"}],\"financial_institution_id\":2,\"financial_institution\":\"Default\",\"trace\":{\"key\":\"iiodknpfk33i57zmghij\"},\"custom_data\":null,\"cuid\":\"iiodknpfk33i57zmghij\",\"username\":\"iiodknpfk33i57zmghij\"}",
{"x-cardsavr-authorization":"SWCH-HMAC-SHA256 Credentials=cardupdatr_ux_integrator",
"x-cardsavr-nonce":"1695884151469",
"x-cardsavr-signature":"IvP5sku4yDw9ALA3hkaqodHNNCmPqYEYzfGtwY2ZdNE="}]
*/