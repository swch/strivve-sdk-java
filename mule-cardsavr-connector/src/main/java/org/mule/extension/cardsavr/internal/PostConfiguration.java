package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

@Operations(PostOperations.class)
public class PostConfiguration {
	 
 	@ParameterGroup(name="General Settings")
 	@Parameter
 	@DisplayName("Path")
 	@Example("/test")
 	private String path;
   
 	@Parameter
 	@Optional
 	@DisplayName("Body")
 	private String body;
   
 	@Parameter
 	@Optional
 	@DisplayName("Headers")
 	private String headers;
   
	public String getPath(){ return path; }
	public void setPath(String path) { this.path = path; }
	
	public String getBody(){ return body; }
	public void setBodyD(String body) { this.body = body; }

	public String getHeaders(){ return headers; }
	public void setHeaders(String headers) { this.headers = headers; }



}
