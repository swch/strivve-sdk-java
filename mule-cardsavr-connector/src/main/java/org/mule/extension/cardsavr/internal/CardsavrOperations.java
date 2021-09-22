package org.mule.extension.cardsavr.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import com.strivve.CardsavrSession;
import com.strivve.CardsavrRESTException;

import java.io.IOException;

import javax.json.JsonValue;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class CardsavrOperations {
	

	static Logger log = LogManager.getLogger(CardsavrOperations.class.getName());
		
	@MediaType(value = ANY, strict = false)
	@Alias("Merchants")
	public String getMerchants(@Connection CardsavrSession connection) {

		log.info("Get merchants...");
		
		String merchantPath = "/merchant_sites";
		JsonValue getMerchants = null;
		String response = null;
		List filters = null;
		  
		try {
			getMerchants = connection.get(merchantPath, filters, null);
			log.debug(getMerchants.toString());
			response = getMerchants.toString();
			
		} catch (IOException | CardsavrRESTException e) {
			/** TODO:  Add code here. */
			e.printStackTrace();
		}
		return response;
		
	  }
	   
}
