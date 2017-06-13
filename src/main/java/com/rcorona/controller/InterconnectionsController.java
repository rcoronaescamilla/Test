package com.rcorona.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.rcorona.model.Flight;
import com.rcorona.model.Leg;
import com.rcorona.service.FlightService;
import com.rcorona.service.FlightServiceImpl;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController
public class InterconnectionsController {
	
	@Autowired
    FlightService flightService;  //Service which will do all data retrieval
    
    @RequestMapping("/interconnections")
    public List<Flight> interconnections
    			(@RequestParam(value="departure") String param1,
    			 @RequestParam(value="arrival") String param2,
    			 @RequestParam(value="departureDateTime") String param3,
    			 @RequestParam(value="arrivalDateTime") String param4) {
    	
    	flightService=new FlightServiceImpl();
        List<Flight> flights = flightService.findAllFlights(param1, param2, param3, param4);
        
        return flights;
    }

    
    
    
    
    
    
    
    
    
    
    
}
