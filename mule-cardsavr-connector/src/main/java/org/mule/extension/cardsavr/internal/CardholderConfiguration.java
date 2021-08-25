package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

@Operations(CardholderOperations.class)
public class CardholderConfiguration {
	 
 	@ParameterGroup(name="Cardholder")
 	@Parameter
 	@DisplayName("Cardholder CUID")
 	@Example("SDFJKKAKDJ")
 	private String cardholder_cuid;
   
 	@Parameter
 	@Optional
 	@DisplayName("Financial Institution")
 	private Integer fi;
   
 	@Parameter
 	@Optional
 	@DisplayName("Type")
 	private String cardholder_type;

 	@Parameter
 	@Optional
 	@DisplayName("First Name")
 	private String cardholder_first_name;

 	@Parameter
 	@Optional
 	@DisplayName("Last Name")
 	private String cardholder_last_name;

 	@Parameter
 	@Optional
 	@DisplayName("Email")
 	private String email;

 	@Parameter
 	@Optional
 	@DisplayName("Meta Key")
 	private String meta_key;

 	@Parameter
 	@Optional
 	@DisplayName("Custom Data")
 	private String custom_data;
 	
 	@Parameter
 	@Optional
 	@DisplayName("Cardholder ID")
 	private String cardholder_id;
 	
	public String getCardholderCUID(){ return cardholder_cuid; }
	public void setCardholderCUID(String cardholder_cuid) { this.cardholder_cuid = cardholder_cuid; }
	
	public Integer getFI(){ return fi; }
	public void setFI(Integer fi) { this.fi = fi; }

	public String getCarholderType(){ return cardholder_type; }
	public void setCarholderType(String cardholder_type) { this.cardholder_type = cardholder_type; }

	
	public String getCarholderFirstName(){ return cardholder_first_name; }
	public void setCarholderFirstName(String cardholder_first_name) { this.cardholder_first_name = cardholder_first_name; }
	
	public String getCarholderLastName(){ return cardholder_last_name; }
	public void setCarholderLastName(String cardholder_last_name) { this.cardholder_last_name = cardholder_last_name; }
	
	public String getEmail(){ return email; }
	public void setCardholderEmail(String email) { this.email = email; }
	
	public String getCarholderMetaKey(){ return meta_key; }
	public void setCarholderMetaKey(String meta_key) { this.meta_key = meta_key; }
	
	public String getCarholderCustomData(){ return custom_data; }
	public void setCarholderCustomData(String custom_data) { this.custom_data = custom_data; }
}
