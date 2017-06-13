package com.rcorona.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

	
	public class FlightUtils {
		
	public static final String formatDate = "yyyy-MM-dd'T'HH:mm";
	
	public FlightUtils(){}
	
	
	public Calendar calculateCalendar(String dateStr) {
		
		SimpleDateFormat formatText = new SimpleDateFormat(formatDate);	         
        Date date = null;
        Calendar calpDate = Calendar.getInstance();
        
        try {

        date = formatText.parse(dateStr);	   
        calpDate.setTime(date);

        } catch (ParseException ex) {
        ex.printStackTrace();
        }
		
		return calpDate;
	} 

}
