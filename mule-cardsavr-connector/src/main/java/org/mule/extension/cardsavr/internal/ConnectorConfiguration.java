package org.mule.extension.cardsavr.internal;

import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;


@Operations({AddressOperations.class, CardOperations.class, CardholderOperations.class, CardsavrOperations.class, JobPostOperations.class})
@ConnectionProviders(CardsavrConnectionProvider.class)
public class ConnectorConfiguration {
	 
	@Parameter
 	@DisplayName("Client Application Name")
 	@Example("MyOrganization Application")
 	private String appName;
	
	@Parameter
 	@DisplayName("Trace Value")
 	@Example("mule-connector")
 	private String trace;
	
	@Parameter
 	@DisplayName("Integrator Name")
 	@Example("mule-connector")
 	private String intName;
   
 	@Parameter
 	@Optional
 	@Password
 	@DisplayName("Integrator Key")
 	private String intKey;
   
 	@Parameter
 	@DisplayName("API Server URL")
 	@Example("api.customer-dev.cardsavr.io")
 	private String apiServerHost;
   
 	@Parameter
 	@DisplayName("Username")
 	@Example("mule-connector-user")
 	private String uName;
   
 	@Parameter
 	@DisplayName("Password")
 	@Password
 	private String uPassword;
 	
 	@Parameter
 	@Example("Default")
 	@DisplayName("Financial Institution")
 	private String fi;
   
 	@Parameter
 	@Optional
 	@DisplayName("Proxy URL")
 	@Placement(order = 10, tab="Proxy")
 	private String proxyServerHost;
   
 	@Parameter
 	@Optional
 	@DisplayName("Proxy Username")
 	@Placement(order = 20, tab="Proxy")
 	private String proxyUsername;
 	
 	@Parameter
 	@Optional
 	@DisplayName("Proxy Password")
 	@Placement(order = 21, tab="Proxy")
 	private String proxyPassword;
	
 	public String getappName(){ return appName; }
	public void setappName(String appName) { this.appName = appName; }
	
	public String getTrace(){ return trace; }
	public void setTrace(String trace) { this.trace = trace; }
	
	public String getintegratorName(){ return intName; }
	public void setintegratorName(String intName) { this.intName = intName; }
	
	public String getintegratorKey(){ return intKey; }
	public void setintegratorKey(String intKey) { this.intKey = intKey; }

	public String getAPIServer(){ return apiServerHost; }
	public void setAPIServer(String apiServerHost) { this.apiServerHost = apiServerHost; }

	public String getuserName(){ return uName; }
	public void setuserName(String uName) { this.uName = uName; }

	public String getuserPassword(){ return uPassword; }
	public void setuserPassword(String uPassword) { this.uPassword = uPassword; }
	
	public String getFI(){ return fi; }
	public void setFI(String fi) { this.fi = fi; }
	
	public String getProxyServer(){ return proxyServerHost; }
	public void setProxyServer(String proxyServerHost) { this.proxyServerHost = proxyServerHost; }

	public String getProxyUsername(){ return proxyUsername; }
	public void setProxyUsername(String proxyUsername) { this.proxyUsername = proxyUsername; }
	
	public String getProxyPassword(){ return proxyPassword; }
	public void setProxyPassword(String proxyPassword) { this.proxyPassword = proxyPassword; }
	

}
