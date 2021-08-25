package org.mule.extension.cardsavr.internal;

import java.io.IOException;

import com.strivve.CardsavrSession;
import com.strivve.CardsavrRESTException;

import javax.json.*;
import javax.json.JsonObject;

import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class CardsavrConnectionProvider implements ConnectionProvider<CardsavrSession> {

	static Logger log = LogManager.getLogger(CardsavrConnectionProvider.class.getName());
	
	  @ParameterGroup(name="Connection")
	  ConnectorConfiguration conf;
	
  @Override
  public CardsavrSession connect() throws ConnectionException {

	  CardsavrSession session = null;
	  JsonObject loginObj = null;
	  UsernamePasswordCredentials proxycreds = null;
	  HttpHost proxyHost = null;
	  
	  
	  String integratorName = conf.getintegratorName();
	  String integratorKey = conf.getintegratorKey();
	  HttpHost apiServer = new HttpHost(conf.getAPIServer());
	  UsernamePasswordCredentials creds = new UsernamePasswordCredentials(conf.getuserName(), conf.getuserPassword()); 
	  
	  String fi = conf.getFI();
	  
	  /** Proxy support. */
	  String proxyServer = conf.getProxyServer();
	  if (proxyServer != null) {
		  proxyHost = new HttpHost(proxyServer);
		  proxycreds = new UsernamePasswordCredentials(conf.getProxyUsername(), conf.getProxyPassword());
	  }
	   
	  /** Create trace object. */
	  String my_trace = conf.getTrace();
	  JsonObject trace;
	  if (my_trace != null) {
		  trace = Json.createObjectBuilder().add("key", my_trace).build();
	  } else {
		  trace = Json.createObjectBuilder().add("key", "mule_connector").build();
	  }
	  
	  /** Get version information for the header. */
	  /** TODO:  Get the the muleconnectorversion version from the package */
	  String muleconnectorversion = getClass().getPackage().getImplementationVersion();
	  muleconnectorversion = "1.0.0";
	  
	  /** TODO:  Get the the sdk version from the repo */
	  String sdkversion = getClass().getPackage().getImplementationVersion();
	  sdkversion = "1.0.0";
	  log.debug("Mule Connector v" + muleconnectorversion + ", Strivve Java SDK v" + sdkversion );
	  
	   
	try {
		  log.info("Establishing CardSavr connection.");
		  if (proxyServer != null) {
			  
			
			session = CardsavrSession.createSession(integratorName, integratorKey, apiServer, proxyHost, proxycreds);	
			  loginObj = (JsonObject) session.login(creds, trace);
			  
		  } else {
			  session = CardsavrSession.createSession(integratorName, integratorKey, apiServer);	
			  loginObj = (JsonObject) session.login(creds, trace);
		   }
		} catch (IOException | CardsavrRESTException e) {
			log.error("Execption connecting to CardSavr.", e); 
		}
	
		/** Set headers */
		/** TODO:  Set the version header */
		CardsavrSession.APIHeaders headers = session.createHeaders();
		headers.trace = trace;
		if (fi != null) {
			headers.financialInsitution = fi;
		} else {
			headers.financialInsitution = "default";
		  		
		}

	  	log.debug(loginObj.toString());
		
	  	return session;

  	}

  @Override
  public void disconnect(CardsavrSession connection) {
	try {
		log.info("Disconnecting from Cardsavr.");  
		connection.end();
	    } catch (Exception e) {
	    	log.error("Error while disconnecting ", e);
	    }
  	}

  @Override
  public ConnectionValidationResult validate(CardsavrSession connection) {
	  log.info("Validating Cardsavr connection."); 
	  if (connection != null ) {
		  log.debug("Connection validated.");
		  return ConnectionValidationResult.success();  
		  }
	  else {  
		  log.debug("Connection failed validation.");
		  return ConnectionValidationResult.failure("Connection is not valid", new ConnectionException("ConnectionException"));
		  
		  }
		  
  	}
  
}