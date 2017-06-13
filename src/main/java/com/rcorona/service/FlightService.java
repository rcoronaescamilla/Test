package com.rcorona.service;

import java.util.List;
import com.rcorona.model.Flight;

public interface FlightService {	
	
	List<Flight> findAllFlights(String dep,String arr,String depdate,String arrDate); 	
}
