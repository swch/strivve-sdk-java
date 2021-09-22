package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

@Operations(JobPostOperations.class)
public class JobPostConfiguration {
	 
 	@ParameterGroup(name="Job Post Settings")
 	@Parameter
 	@Optional
 	@DisplayName("Cardholder CUID")
 	@Example("SDFJKKAKDJ")
 	private String carholder_cuid;
   
 	@Parameter
 	@Optional
 	@DisplayName("Card")
 	private Integer card;
   
 	@Parameter
 	@Optional
 	@DisplayName("Address")
 	private String address;
 	
	public String getCardholderCUID(){ return carholder_cuid; }
	public void setCardholderCUID(String carholder_cuid) { this.carholder_cuid = carholder_cuid; }
	
	public Integer getCard(){ return card; }
	public void setCard(Integer card) { this.card = card; }

	public String getAddress(){ return address; }
	public void setAddress(String address) { this.address = address; }

}
