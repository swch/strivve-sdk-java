package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

@Operations(GetOperations.class)
public class GetConfiguration {
	 
 	@ParameterGroup(name="General Settings")
 	@Parameter
 	@DisplayName("Path")
 	@Example("/merchant_sites")
 	private String path;
   
 	@Parameter
 	@Optional
 	@DisplayName("ID")
 	private Integer id;
   
 	@Parameter
 	@Optional
 	@DisplayName("filters")
 	private String filters;
   
 	@Parameter
 	@Optional
 	@DisplayName("headers")
 	private String headers;
 	
	public String getPath(){ return path; }
	public void setPath(String path) { this.path = path; }
	
	public Integer getID(){ return id; }
	public void setID(Integer id) { this.id = id; }

	public String getFilters(){ return filters; }
	public void setFilters(String filters) { this.filters = filters; }

	public String getHeaders(){ return headers; }
	public void setHeaders(String headers) { this.headers = headers; }



}
