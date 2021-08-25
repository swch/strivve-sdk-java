package org.mule.extension.cardsavr.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import com.strivve.CardsavrSession;
import com.strivve.CardsavrRESTException;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;

/**
 * This class is a container for operations.
 * Every public method in this class will be taken as an extension operation.
 * Use @ignore to keep a public method from being listed as an extension operation.
 */

public class CardholderOperations {
	
	static Logger log = LogManager.getLogger(CardholderOperations.class.getName());
	
	String path = "/cardholders";
		
	@MediaType(value = ANY, strict = false)
	@Alias("cardholderCreate")
	public String cardholderCreate(@Connection CardsavrSession connection, String cardholder_cuid, Integer fi, String cardholder_type, String cardholder_first_name, String cardholder_last_name, String email, String meta_key, String custom_data) {
		
		JsonObject response = null;

		JsonObject jsonobj = (JsonObject) Json.createObjectBuilder()
			     .add("cuid", "cardholder_cuid")
			     .build();
		
		CardsavrSession.APIHeaders headers = connection.createHeaders();
        headers.financialInsitution = "default";
        
        
			try {
				log.info("Create Cardholder...");
				response = (JsonObject) connection.post(path, jsonobj, headers);
				log.debug(response.toString());
								
			} catch (IOException | CardsavrRESTException e) {

				log.debug(e);
			}	
					
			return response.toString();
		
	  }
	

	@MediaType(value = ANY, strict = false)
	@Alias("cardholderDelete")
	public String cardholderDelete(@Connection CardsavrSession connection, Integer cardholder_id) {
		
		JsonObject response = null;

		CardsavrSession.APIHeaders headers = connection.createHeaders();
        headers.financialInsitution = "default";
                
			try {
				log.info("Delete Cardholder...");
				response = (JsonObject) connection.delete(path, cardholder_id, headers);
				log.debug(response.toString());
								
			} catch (IOException | CardsavrRESTException e) {

				log.debug(e);

			}	
			
			return response.toString();
		
	  }
}
