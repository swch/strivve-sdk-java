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

public class AddressOperations {
	
	static Logger log = LogManager.getLogger(AddressOperations.class.getName());
	
	String path = "/cardsavr_addresses";
		
	@MediaType(value = ANY, strict = false)
	@Alias("addressCreate")
	public String addressCreate(@Connection CardsavrSession connection, Integer cardholder_id, String address1, String city, String subnational, String postal_code, String is_primary) {
		
		JsonObject response = null;

		JsonObject jsonobj = (JsonObject) Json.createObjectBuilder()
			     .add("id", cardholder_id)
			     .add("address1", address1)
			     .add("city", city)
			     .add("subnational", subnational)
			     .add("postal_code", postal_code)
			     .add("is_primary", is_primary)
			     .build();
		
		CardsavrSession.APIHeaders headers = connection.createHeaders();
        headers.financialInsitution = "default";
        
        
			try {
				log.info("Create Address...");
				response = (JsonObject) connection.post(path, jsonobj, headers);
				log.debug(response.toString());
								
			} catch (IOException | CardsavrRESTException e) {
				log.debug(e);
			}	
					
			return response.toString();
		
	  }
	

	@MediaType(value = ANY, strict = false)
	@Alias("addressDelete")
	public String addressDelete(@Connection CardsavrSession connection, Integer address_id) {
		
		JsonObject response = null;

		CardsavrSession.APIHeaders headers = connection.createHeaders();
        headers.financialInsitution = "default";
                
			try {
				log.info("Delete Address...");
				response = (JsonObject) connection.delete(path, address_id, headers);
				log.debug(response.toString());
								
			} catch (IOException | CardsavrRESTException e) {
				log.debug(e);

			}	
			
		
			return response.toString();
		
	  }
}
