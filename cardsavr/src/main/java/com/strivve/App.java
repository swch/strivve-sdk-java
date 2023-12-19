package com.strivve;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.strivve.Signing;

public class App {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            String input = new BufferedReader(
                new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            Headers headers = t.getRequestHeaders();
            String signature = headers.getFirst("x-cardsavr-signature");
            String nonce = headers.getFirst("x-cardsavr-nonce");
            String authorization = headers.getFirst("x-cardsavr-authorization");
            String response = "Response is not verified";
            if (signature != null && nonce != null && authorization != null) {
                try {
                    Signing.verifySignature(
                        signature, 
                        "http://"+t.getRequestHeaders().getFirst("Host")+t.getRequestURI(),
                        authorization, 
                        nonce, 
                        new SecretKey[] {Encryption.convertRawToAESKey(Base64.getDecoder().decode("KfqTiEYTNWoDMkiZlGmaxNiaZnsmvrW9qxVPd2pb9M0="))}, 
                        input);
                    response = "Response IS verified";
                } catch (Exception e) {
                    // Signature verification failed
                    e.printStackTrace();
                }
            }
            System.out.println(response);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}


//{"jobs":[{"job_id":1280,"completed_on":"2023-07-31T08:38:32.149Z","merchant_site":{"id":1,"host":"synthetic-sites-server.vercel.app/index.html","name":"SS Standard Login"},"termination_type":"BILLABLE","job_status_message":"Your card was placed successfully.","job_status":"SUCCESSFUL","custom_data":{"spa_parid":"00F071","type":"Card_On_File_Placement","username":"test_username","application_id":1111,"sub_application_id":2222,"token_id":"333333333333333333333333333"},"card_customer_key":"00000000000000000001690792682"}],"financial_institution_id":2,"financial_institution":"Default","trace":{"key":"1690792682"},"custom_data":{"spa_parid":"00F071","type":"Card_On_File_Placement","username":"test_username","application_id":1111,"sub_application_id":2222,"token_id":"333333333333333333333333333"},"cuid":"1690792682","username":"1690792682"}

