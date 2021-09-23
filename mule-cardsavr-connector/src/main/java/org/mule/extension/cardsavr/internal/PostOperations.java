package org.mule.extension.cardsavr.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import com.strivve.CardsavrSession;
import com.strivve.CardsavrRESTException;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonValue;

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

public class PostOperations {
	
	static Logger log = LogManager.getLogger(PostOperations.class.getName());
		
	@MediaType(value = ANY, strict = false)
	@Alias("POST")
	public String get(@Connection CardsavrSession connection, String path, String b) {
		
		/** TODO:  Add support for filters */
		

		/** TODO:  Add support for headers */
		String response = "No id provided.";
		JsonValue results = null;
		
		/** TODO: Add the body parameters */
		JsonObject body = null;
		
			try {
				log.info("GET...");
				results = connection.post(path, body, null);
				log.debug(results.toString());
								
			} catch (IOException | CardsavrRESTException e) {
				/** TODO:  Add code here. */
				e.printStackTrace();
			}	
			
		
			return response;
		
	  }
	

}
