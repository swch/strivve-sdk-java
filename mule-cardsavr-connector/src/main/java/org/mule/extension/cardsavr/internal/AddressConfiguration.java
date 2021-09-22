package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

@Operations(CardholderOperations.class)
public class AddressConfiguration {
	 
 	@ParameterGroup(name="Address")
 	@Parameter
 	@DisplayName("Cardholder ID")
 	@Example("123")
 	private Integer cardholder_id;
   
 	@Parameter
 	@Optional
 	@DisplayName("Address Line 1")
 	private String address1;
   
 	@Parameter
 	@Optional
 	@DisplayName("City")
 	private String city;

 	@Parameter
 	@Optional
 	@DisplayName("Subnational")
 	private String subnational;

 	@Parameter
 	@Optional
 	@DisplayName("postal_code")
 	private String postal_code;

 	@Parameter
 	@Optional
 	@DisplayName("Primay")
 	private String is_primary;
 	
 	@Parameter
 	@DisplayName("Adress ID")
 	@Example("123")
 	private Integer address_id;

 	
	public Integer getCardholderID(){ return cardholder_id; }
	public void setCardholderID(Integer cardholder_id) { this.cardholder_id = cardholder_id; }
	
	public String getAddressLine1(){ return address1; }
	public void setAddressLine1(String address1) { this.address1 = address1; }

	public String getAddressCity(){ return city; }
	public void setAddressCity(String city) { this.city = city; }

	
	public String getAddressSubnational(){ return subnational; }
	public void getAddressSubnational(String subnational) { this.subnational = subnational; }
	
	public String getAddressPostalCode(){ return postal_code; }
	public void setAddressPostalCode(String postal_code) { this.postal_code = postal_code; }
	
	public String getAddressIsPrimary(){ return is_primary; }
	public void setAddressIsPrimary(String is_primary) { this.is_primary = is_primary; }
	
	public Integer getAddressID(){ return address_id; }
	public void setAddressID(Integer address_id) { this.address_id = address_id; }
	
}
