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

public class CardOperations {
	
	static Logger log = LogManager.getLogger(CardOperations.class.getName());
	
	String path = "/cardsavr_cards";
		
	@MediaType(value = ANY, strict = false)
	@Alias("cardCreate")
	public String cardCreate(@Connection CardsavrSession connection, Integer cardholder_id, String par, String pan, String cvv, String expiration_month, String expiration_year, String name_on_card) {
		
		JsonObject response = null;

		JsonObject jsonobj = (JsonObject) Json.createObjectBuilder()
			     .add("id", cardholder_id)
			     .add("par", par)
			     .add("pan", pan)
			     .add("cvv", cvv)
			     .add("expiration_month", expiration_month)
			     .add("expiration_year", expiration_year)
			     .add("name_on_card", name_on_card)
			     .build();
		
		CardsavrSession.APIHeaders headers = connection.createHeaders();
        headers.financialInsitution = "default";
        
        
			try {
				log.info("Create Card...");
				response = (JsonObject) connection.post(path, jsonobj, headers);
				log.debug(response.toString());
								
			} catch (IOException | CardsavrRESTException e) {
				log.debug(e);
			}	
					
			return response.toString();
		
	  }
	

	@MediaType(value = ANY, strict = false)
	@Alias("cardDelete")
	public String cardDelete(@Connection CardsavrSession connection, Integer card_id) {
		
		JsonObject response = null;

		CardsavrSession.APIHeaders headers = connection.createHeaders();
        headers.financialInsitution = "default";
                
			try {
				log.info("Delete Card...");
				response = (JsonObject) connection.delete(path, card_id, headers);
				log.debug(response.toString());
								
			} catch (IOException | CardsavrRESTException e) {
				
				log.debug(e);

			}	
			
		
			return response.toString();
		
	  }
}
