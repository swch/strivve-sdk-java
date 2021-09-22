package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.Parameter;


@Operations(CardsavrOperations.class)
@ConnectionProviders(CardsavrConnectionProvider.class)
public class CardsavrConfiguration {
	 
 	@Parameter
 	@DisplayName("Unit ID")
 	@Example("Engineering")
 	private String unitID;
   
	public String setunitID(){ return unitID; }
	public void getunitID(String unitID) { this.unitID = unitID; }
	
}
