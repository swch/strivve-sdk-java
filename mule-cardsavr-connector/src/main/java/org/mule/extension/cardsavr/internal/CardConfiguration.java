package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

@Operations(CardholderOperations.class)
public class CardConfiguration {
	 
 	@ParameterGroup(name="Cardholder")
 	@Parameter
 	@DisplayName("Cardholder ID")
 	@Example("123")
 	private Integer cardholder_id;
   
 	@Parameter
 	@Optional
 	@DisplayName("PAR")
 	private String par;
   
 	@Parameter
 	@Optional
 	@DisplayName("PAN")
 	private String pan;

 	@Parameter
 	@Optional
 	@DisplayName("CVV")
 	private String cvv;

 	@Parameter
 	@Optional
 	@DisplayName("expiration_month Name")
 	private String expiration_month;

 	@Parameter
 	@Optional
 	@DisplayName("expiration_year")
 	private String expiration_year;

 	@Parameter
 	@Optional
 	@DisplayName("Name On Card")
 	private String name_on_card;
 	
 	@Parameter
 	@Optional
 	@DisplayName("Card ID")
 	private Integer card_id;
 	
	public Integer getCardholderID(){ return cardholder_id; }
	public void setCardholderID(Integer cardholder_id) { this.cardholder_id = cardholder_id; }
	
	public String getCardPAR(){ return par; }
	public void setCardPAR(String par) { this.par = par; }

	public String getCardPAN(){ return pan; }
	public void setCardPAN(String pan) { this.pan = pan; }
	
	public String getCardCVV(){ return cvv; }
	public void setCardCVV(String cvv) { this.cvv = cvv; }
	
	public String getCardExpirationMonth(){ return expiration_month; }
	public void setCardExpirationMonth(String expiration_month) { this.expiration_month = expiration_month; }
	
	public String getCardExpirationYear(){ return expiration_year; }
	public void setCardExpirationYear(String expiration_year) { this.expiration_year = expiration_year; }
	
	public String getCardNameOnCard(){ return name_on_card; }
	public void setCardNameOnCard(String name_on_card) { this.name_on_card = name_on_card; }
	
	public Integer getCardID(){ return card_id; }
	public void setCardID(Integer card_id) { this.card_id = card_id; }
}
