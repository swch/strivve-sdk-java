package org.mule.extension.cardsavr.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import com.strivve.CardsavrSession;
import com.strivve.CardsavrRESTException;

import java.io.IOException;

import javax.json.JsonValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * This class is a container for operations.
 * Every public method in this class will be taken as an extension operation.
 * Use @ignore to keep a public method from being listed as an extension operation.
 */

public class GetOperations {
	
	static Logger log = LogManager.getLogger(GetOperations.class.getName());
		
	@MediaType(value = ANY, strict = false)
	@Alias("GET")
	public String get(@Connection CardsavrSession connection, String path, @Optional Integer id, @Optional String filters) {
		
		String response = "No id provided.";
		JsonValue results = null;
				
		if (id != null) {
			try {
				log.info("GET...");
				results = connection.get(path, id, null);
				log.debug(results.toString());
								
			} catch (IOException | CardsavrRESTException e) {
				e.printStackTrace();
			}	
			
		} else {
			return response;
		}
		  
		return results.toString();
		
	  }
	

}
