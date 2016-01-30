package com.rmq.pds;

import java.net.URI;
import java.net.URISyntaxException;

//Utility class to help on some operations
public class Utils {
	
	//creates a url string from the address in the form ip:port
	public static String toUrlString(String socketAddress) {
		return "http://" + socketAddress + "/xmlrpc";
	} // toUrlString
	
	private static URI toUri(String socketAddress) throws URISyntaxException
	{
		return new URI(toUrlString(socketAddress));
	} // toURI
	
	//for validation of socket address
	public static boolean isValidSocketAddress(String socketAddress)
	{
		try {
			  URI uri = toUri(socketAddress); // may throw URISyntaxException

			  if (uri.getHost() == null || uri.getPort() < 0) 
			  {
			    throw new URISyntaxException(uri.toString(),
			      "Socket address must have host and port parts");
			  } // if
			  else
			  {
				  return true;
			  } // else
			  
			} catch (URISyntaxException ex) {
			  System.out.println(ex.getMessage());
			  return false;
			} // catch
	} // isValidSocketAddress
	
	//create a socket address from IP address and port
	public static String toSocketAddress(String IP, int port)
	{
		return IP + ":" + port;
	} // toSocketAddress
	
	//split the socket address and returns port
	public static int getPortFromSocketAddress(String socketAddress)
	{	
		//split this string into IP and port
		String[] addressComponents = socketAddress.split(":");
		
		//get the ip and port
		//String IP = addressComponents[0];
		int port = Integer.parseInt(addressComponents[1]);
		
		return port;
	} // getPortFromSocketAddress
	
	//split the socket address and returns IP
	public static String getIPFromSocketAddress(String socketAddress)
	{	
		//split this string into IP and port
		String[] addressComponents = socketAddress.split(":");
		
		//get the ip and port
		String IP = addressComponents[0];
		//int port = Integer.parseInt(addressComponents[1]);
		
		return IP;
	} // getIPFromSocketAddress
	
	//method to generate a random socket address for a client
	//for the client, server's socket address is important
	public static String getDefaultSocketAddress()
	{
		return "localhost" + ":" + 8080;
	} // getDefaultSocketAddress
	
	////method to generate a random ip address for a client
	//for the client, server's socket address is important
	public static String getDefaultIP()
	{
		return "localhost";
	} // getDefaultIP
	
	//for Logging
	public static String logTimestamp(int time) {
		return "Thread " + Thread.currentThread().getId() + " with timemstamp = " + time + " ticks: ";
	}
} // class Utils
