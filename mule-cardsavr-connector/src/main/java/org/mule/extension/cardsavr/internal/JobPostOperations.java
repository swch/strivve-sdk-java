package org.mule.extension.cardsavr.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import com.strivve.CardsavrSession;
import com.strivve.CardsavrRESTException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.lang3.RandomStringUtils;
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

public class JobPostOperations {
	
	static Logger log = LogManager.getLogger(JobPostOperations.class.getName());
		
	@MediaType(value = ANY, strict = false)
	@Alias("Job_POST")
	public String jobPost(@Connection CardsavrSession connection) {
		
		JsonObject jsonobj = null;
		JsonObject response = null;
				
		try {
			String data = new String(Files.readAllBytes(Paths.get("src\\test\\resources\\job_data.json")), StandardCharsets.UTF_8)
	                .replaceAll("\\{\\{CARDHOLDER_UNIQUE_KEY\\}\\}", RandomStringUtils.random(6, true, true));
	        jsonobj = Json.createReader(new StringReader(data)).read().asJsonObject();
			} catch (IOException e) {

				log.debug(e);
			}	
		
		CardsavrSession.APIHeaders headers = connection.createHeaders();
        
        
        
			try {
				log.info("Post Job...");
				response = (JsonObject) connection.post("/place_card_on_single_site_jobs", jsonobj, headers);
				log.debug(response.toString());
								
			} catch (IOException | CardsavrRESTException e) {
				/** TODO:  Add code here. */
				e.printStackTrace();
			}	
			
		
			return response.toString();
		
	  }
	

}
