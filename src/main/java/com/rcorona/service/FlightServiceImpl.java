package com.rcorona.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import java.util.LinkedHashMap;
import java.util.List;


import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.rcorona.model.Flight;
import com.rcorona.model.Leg;
import com.rcorona.utils.FlightUtils;

@Repository
@Service("flightService")
public class FlightServiceImpl implements FlightService{
	
	public static final String FIRST_MICROSERVICE_URI = "https://api.ryanair.com/core/3/routes/";
	public static final String SECOND_MICROSERVICE_URI ="https://api.ryanair.com/timetable/3/schedules/{departure}/{arrival}/years/{year}/months/{month}";

		
	public List<Flight> findAllFlights(String departure, String arrival, String depDateStr, String arrDateStr) {

		
		FlightUtils fUtils=new FlightUtils();
		List<Flight> allFlights=new ArrayList<>();
		
		
		//Call first microservice to get the routes
		
		List iatasWithOriginDeparture=new ArrayList();
		List iatasWithDestinyArrival=new ArrayList();
		List iatasScales=null;
		
		RestTemplate restTemplate;
		boolean isNeccesaryProxy=false; 
		
		if (isNeccesaryProxy){			
			restTemplate = setProxy();			
		}else{			
			restTemplate = new RestTemplate();
		}
		 
	
		List<LinkedHashMap<String, Object>> routesMap = restTemplate.getForObject(FIRST_MICROSERVICE_URI, List.class);

		if(routesMap!=null){
			
				//Calculate Interconnections IATA codes (iatasScales)
			
	            calculatePossibleInterConnections(departure, 
	            								  arrival, 
							            		  iatasWithOriginDeparture, 
							            		  iatasWithDestinyArrival,
												  routesMap);
	            
	            iatasScales=iatasWithDestinyArrival;
	           
	             
	            //CALCULATE DIRECT FLIGHTS WITH THE SECOND MICROSERVICE------------------------------------------------------
	        	          
	            List<Leg> arrLegsDirect=new ArrayList<>();
	            
	            String origin=departure;
	            String destiny=arrival;
	            
	    		Calendar caldepDate=fUtils.calculateCalendar(depDateStr);
	    		Calendar calarrDate=fUtils.calculateCalendar(arrDateStr);
	    	            
	    		//calculate flights for the date
	            List<LinkedHashMap<String, Object>> flights=searchFlights(origin,destiny,caldepDate,calarrDate,restTemplate);     
	      
	            if ((flights!=null)&&(flights.size()>0)){
	            	
		            //filter flights with the restrictions	            
	            	List<LinkedHashMap<String, Object>> flightsFiltered=filterFlights(flights,depDateStr,arrDateStr,caldepDate,calarrDate,fUtils);
		         	            
		            //calculate legs from the flights
		            arrLegsDirect=getLegs(departure, arrival, depDateStr, arrDateStr,flightsFiltered,fUtils);
		          	            
					//Sort the Legs by Date
					sortLegs(arrLegsDirect,fUtils);
			 		
					//Add new flight to the final List
					addFlight(allFlights,arrLegsDirect,0);
	            }
	            
	         
	            
				
				//CALCULATE WITH SCALE FLIGHTS WITH THE SECOND MICROSERVICE------------------------------------------------------
				
				//Loop the Interconnections IATAS codes			
				
				for (int i=0;i<iatasScales.size();i++){					
					
					//Interconnection IATA code
					String scale=(String) iatasScales.get(i);		
					
					//CALCULATE FLIGHTS BETWEEN ORIGIN AND SCALE					
					//calculate flights for the date 
					getInterconnectedFlights(departure, arrival, depDateStr, arrDateStr, fUtils, allFlights,
							restTemplate, origin, destiny, caldepDate, calarrDate, scale);
				
					
				}//End for IATA codes
					
	    }//End if routesMap!=null
		
		return allFlights;
		   
	}

	private void getInterconnectedFlights(String departure, String arrival, String depDateStr, String arrDateStr,
			FlightUtils fUtils, List<Flight> allFlights, RestTemplate restTemplate, String origin, String destiny,
			Calendar caldepDate, Calendar calarrDate, String scale) {
		
		List<LinkedHashMap<String, Object>> flightsDestinyScale=searchFlights(origin,scale,caldepDate,calarrDate,restTemplate);	
		
		if ((flightsDestinyScale!=null)&&(flightsDestinyScale.size()>0)){
			
			 //filter flights with the restrictions	            
			List<LinkedHashMap<String, Object>> flightsDestiny=filterFlights(flightsDestinyScale,depDateStr,arrDateStr,caldepDate,calarrDate,fUtils);
		    
		    if (flightsDestiny.size()>0){	
		    	
		    	//CALCULATE FLIGHTS BETWEEN SCALE AND DESTINY
		    	//calculate flights for the date 
		    	List<LinkedHashMap<String, Object>> flightsOriginScale=searchFlights(scale,destiny,caldepDate,calarrDate,restTemplate);	
		    	if ((flightsOriginScale!=null)&&(flightsOriginScale.size()>0)){
		    		 //filter flights with the restrictions	            
		    		List<LinkedHashMap<String, Object>> flightsOrigin=filterFlights(flightsOriginScale,depDateStr,arrDateStr,caldepDate,calarrDate,fUtils);
		            if (flightsOrigin.size()>0){
		            						            	
		            	//search the pairs that complain with restrictions 2 h or greater
		            	
		            	for(LinkedHashMap<String, Object> mapflightsA : flightsDestiny)
		       		 	{
		            		
		            	 String departureTimeA=(String) mapflightsA.get("departureTime");
		       			 String arrivalTimeA=(String) mapflightsA.get("arrivalTime");					       			 
		       			 String flightArrDateStr=arrDateStr.substring(0,arrDateStr.indexOf("T"));	            			 
		       			 Calendar flightArrDateA=fUtils.calculateCalendar(flightArrDateStr+"T"+arrivalTimeA);
		       			 
		       			 	for(LinkedHashMap<String, Object> mapflightsB : flightsOrigin)
		       			 	{
		       			 		
		       		       	String departureTimeB=(String) mapflightsB.get("departureTime");
			       			String arrivalTimeB=(String) mapflightsB.get("arrivalTime");	
		       			 				       			 
			       			String  flightDepDateStr=depDateStr.substring(0,depDateStr.indexOf("T"));	            			 
			    			Calendar flightDepDateB=fUtils.calculateCalendar(flightDepDateStr+"T"+departureTimeB);
			    			
			    			flightDepDateB.add(Calendar.HOUR_OF_DAY, -2);// subtracts two hours
			    			
			    			 if ((flightDepDateB.compareTo(flightArrDateA)==0) || (flightDepDateB.compareTo(flightArrDateA)>0)){
			    				 //Right combination
			    				 Leg legA=new Leg();
			    				 legA.setDepartureAirport(departure);
			    				 legA.setArrivalAirport(scale);
			    				 legA.setDepartureDateTime(flightDepDateStr+"T"+departureTimeA);
			    				 legA.setArrivalDateTime(flightArrDateStr+"T"+arrivalTimeA);
			    				 
			    				 Leg legB=new Leg();
			    				 legB.setDepartureAirport(scale);
			    				 legB.setArrivalAirport(arrival);
			    				 legB.setDepartureDateTime(flightDepDateStr+"T"+departureTimeB);
			    				 legB.setArrivalDateTime(flightArrDateStr+"T"+arrivalTimeB);
			    				 
			    				 List<Leg> arrayLegsScale=new ArrayList<Leg>();
			    				 arrayLegsScale.add(legA);
			    				 arrayLegsScale.add(legB);
			    				 
			    				//Add new flight to the final List
			 					addFlight(allFlights,arrayLegsScale,1);			    				 
			    			 }
		       			 	}
		       		 	}
		            	
		            } //End if flightOrigenScale>0
		          			            		
		    	} //End if flightOriginScale !=null
		    
		    } //End if flightDestinyScale>0
		 
		} //End if flightDestinyScale!=null
	}

	private List<Leg> getLegs(String departure, String arrival, String depDateStr, String arrDateStr,
			 List<LinkedHashMap<String, Object>> flights,FlightUtils fUtils) {
		
		List<Leg> arrayLegs=new ArrayList<Leg>();

		for(LinkedHashMap<String, Object> mapflights : flights)
		{
		String number=(String) mapflights.get("number");
		String departureTime=(String) mapflights.get("departureTime");
		String arrivalTime=(String) mapflights.get("arrivalTime");


		String  flightDepDateStr=depDateStr.substring(0,depDateStr.indexOf("T"));	            			 
		Calendar flightDepDate=fUtils.calculateCalendar(flightDepDateStr+"T"+departureTime);

		String flightArrDateStr=arrDateStr.substring(0,arrDateStr.indexOf("T"));	            			 
		Calendar flightArrDate=fUtils.calculateCalendar(flightArrDateStr+"T"+arrivalTime);
		
		 Leg legdirect=new Leg();
		 legdirect.setDepartureAirport(departure);
		 legdirect.setArrivalAirport(arrival);
		 legdirect.setDepartureDateTime(flightDepDateStr+"T"+departureTime);
		 legdirect.setArrivalDateTime(flightArrDateStr+"T"+arrivalTime);
		 
		 arrayLegs.add(legdirect);	

		 }
		
		return arrayLegs;
	}
	
	private List<LinkedHashMap<String, Object>> searchFlights(
			String origin,
			String destiny,
			Calendar caldepDate,
			Calendar calarrDate,
			RestTemplate restTemplate){
			
		
		String url=SECOND_MICROSERVICE_URI;
		
		url=url.replace("{departure}", origin);
		url=url.replace("{arrival}", destiny);
		
		url=url.replace("{year}", String.valueOf(caldepDate.get(Calendar.YEAR)));
		url=url.replace("{month}",String.valueOf(caldepDate.get(Calendar.MONTH)+1));
		
		try{
			
			LinkedHashMap<String, Object> flightsMap = restTemplate.getForObject(url, LinkedHashMap.class);			
		
			//Call second microservice to get flights with a scale
			
			List<LinkedHashMap<String, Object>> lstFlights=(List) flightsMap.get("days");
			
	
			
			for(LinkedHashMap<String, Object> map : lstFlights){
				int day=(Integer) map.get("day");
	       
				if (day==(caldepDate.get(Calendar.DAY_OF_MONTH))) {
		
					
					return (List)map.get("flights");
		
					
				}
	     	
				
			}
			
		}catch(java.lang.NullPointerException e){
			return null;
		}catch(Exception e){
			return null;
		}
		return null;
		
	}


	private void addFlight(List<Flight> allFlights, List<Leg> arrLegs,int numStop) {
		if (arrLegs.size()>0){
			
			Flight flight=new Flight();
			
			flight.setStops(numStop);	        					 
			flight.setLegs(arrLegs);
			 
			allFlights.add(flight);
		}
	}
	

	private void sortLegs(List<Leg> arrLegsDirect,FlightUtils fUtils ) {
		
		Collections.sort(arrLegsDirect, new java.util.Comparator<Leg>() {
		  
				@Override
				public int compare(Leg leg1, Leg leg2) {
					
																
					Calendar calculateCalendarLeg1 = fUtils.calculateCalendar(leg1.getDepartureDateTime());
					Calendar calculateCalendarLeg2 = fUtils.calculateCalendar(leg2.getDepartureDateTime());
						
					return calculateCalendarLeg1.compareTo(calculateCalendarLeg2);
				
				}
		 });
		
	}

	private List<LinkedHashMap<String, Object>>  filterFlights(
			
								List<LinkedHashMap<String, Object>> flights,
								String depDateStr, 
								String arrDateStr,
								Calendar caldepDate, 
								Calendar calarrDate,
								FlightUtils fUtils){
		
		List<LinkedHashMap<String, Object>> flightsFiltered=new ArrayList<LinkedHashMap<String, Object>>();
				
		for(LinkedHashMap<String, Object> mapflights : flights)
		 {
			 String number=(String) mapflights.get("number");
			 String departureTime=(String) mapflights.get("departureTime");
			 String arrivalTime=(String) mapflights.get("arrivalTime");
			 
			 
			 String  flightDepDateStr=depDateStr.substring(0,depDateStr.indexOf("T"));	            			 
			 Calendar flightDepDate=fUtils.calculateCalendar(flightDepDateStr+"T"+departureTime);
			 
			 String flightArrDateStr=arrDateStr.substring(0,arrDateStr.indexOf("T"));	            			 
			 Calendar flightArrDate=fUtils.calculateCalendar(flightArrDateStr+"T"+arrivalTime);
			 
		        		
			 if ((flightDepDate.compareTo(caldepDate)==0) || (flightDepDate.compareTo(caldepDate)>0)){
				 if ((flightArrDate.compareTo(calarrDate)==0) || (flightArrDate.compareTo(calarrDate)<0)){
					 flightsFiltered.add(mapflights);

				 }
			
			 }
			 
		 }
		
		return flightsFiltered;
	}


	private void calculatePossibleInterConnections(String departure, String arrival, List iatasWithOriginDeparture,
			List iatasWithDestinyArrival, List<LinkedHashMap<String, Object>> routesMap) {
		
			for(LinkedHashMap<String, Object> map : routesMap){
				String airportFrom=(String) map.get("airportFrom");
				String airportTo=(String) map.get("airportTo");	            	
				if (airportTo.equals(arrival))iatasWithDestinyArrival.add(airportFrom);
				if (airportFrom.equals(departure)) iatasWithOriginDeparture.add(airportTo);            	
				
			}
   
			iatasWithDestinyArrival.retainAll(iatasWithOriginDeparture);
	}

	private RestTemplate setProxy() {
		
		String httpProxy="";
		int httpPort=8080;
		String httpUser="";
		String httpPwd="";
		
		RestTemplate restTemplate;
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials( 
		        new AuthScope(httpProxy, httpPort), 
		        new UsernamePasswordCredentials(httpUser, httpPwd));
		
  
   
		HttpHost myProxy = new HttpHost(httpProxy, httpPort);
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		clientBuilder.setProxy(myProxy).setDefaultCredentialsProvider(credsProvider).disableCookieManagement();
		HttpClient httpClient = clientBuilder.build();
		
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setHttpClient(httpClient);
		
		restTemplate = new RestTemplate(factory);
		return restTemplate;
	}


	

}
