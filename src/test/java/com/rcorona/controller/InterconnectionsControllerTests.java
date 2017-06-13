package com.rcorona.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.rcorona.service.FlightService;
import com.rcorona.service.FlightServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc


public class InterconnectionsControllerTests {

    @Autowired
    private MockMvc mockMvc;    
    
    @MockBean
    FlightService flightService;

    @Test    
    public void resultWithParams() throws Exception {
    	
    	
    	String url="/interconnections?departure=DUB&arrival=WRO&departureDateTime=2017-07-02T07:00&arrivalDateTime=2017-07-02T21:00";

        mockMvc.perform(get(url)).andDo(print())            
        		.andExpect(status().isOk())   
        		.andExpect(jsonPath("$[0].stops").value("0"))                   
                .andExpect(jsonPath("$[0].legs[0].departureAirport").value("DUB"))
                .andExpect(jsonPath("$[0].legs[0].arrivalAirport").value("WRO"))
                .andExpect(jsonPath("$[0].legs[0].departureDateTime").value("2017-07-02T17:25"))
                .andExpect(jsonPath("$[0].legs[0].arrivalDateTime").value("2017-07-02T21:00"))
                .andExpect(jsonPath("$[1].stops").value(1))
                .andExpect(jsonPath("$[1].legs[0].departureAirport").value("DUB"))
                .andExpect(jsonPath("$[1].legs[0].arrivalAirport").value("NCL"))
                .andExpect(jsonPath("$[1].legs[0].departureDateTime").value("2017-07-02T07:35"))
                .andExpect(jsonPath("$[1].legs[0].arrivalDateTime").value("2017-07-02T08:45"))
                .andExpect(jsonPath("$[1].legs[1].departureAirport").value("NCL"))
                .andExpect(jsonPath("$[1].legs[1].arrivalAirport").value("WRO"))
                .andExpect(jsonPath("$[1].legs[1].departureDateTime").value("2017-07-02T12:30"))
                .andExpect(jsonPath("$[1].legs[1].arrivalDateTime").value("2017-07-02T15:45"));

    }


}
