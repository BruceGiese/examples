package com.giese.bruce.rmi;

import java.rmi.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This file contains a template for a client application which accesses the server template.
 * The intent is to be able to take the client, server, and interface as a starting point
 * for an RMI remote method invocation application.
 * <p>
 * Note that you need to set up some things on both the client and the server in order for this
 * to work.  On the client, you need the following:
 *    1) Create a policy file and make sure it's in the classpath (let's call it network.policy).
 *       In this example I've placed it in the project root directory.
 *    2) Set the java VM arguments to include -Djava.security.policy=network.policy
 *    3) For logging, create a properties file and make sure it's in the class path
 *       (let's call it logging.properties).
 *    4) For logging, set the java VM arguments to include -Djava.util.logging.config.file="logging.properties"
 *    
 * For the policy file, this was tested with the file network.policy in the project root directory with
 * the following code:
 *    grant {
 *    	permission java.security.AllPermission;
 *    	permission java.net.SocketPermission "127.0.0.1:*", "connect,resolve";
 *    };
 * This is probably very dangerous and open to hackers.
 * 
 * For the properties file, this was tested with the file logging.properties in the project root directory
 * with the following code:
 *    handlers = java.util.logging.ConsoleHandler
 *    java.util.logging.ConsoleHandler.level = ALL
 * 
 * @author		Bruce Giese
 * @version		0.1
 */
public class ClientTemplate {
	static final String DEFAULT_HOST_ADDRESS = "192.168.1.5";
	static final String DEFAULT_SERVER_NAME = "TemplateServer";
	static final int DEFAULT_PORT = 1099;
	// TODO find the right way to define and share this response between the client and server
	static final String EXPECTED_RESPONSE = "The server successfully received: ";
	static final int ITERATIONS = 4;
	
	public ClientTemplate() {
		super();		/* This is completely unnecessary */
	}

	/**
	 * usage: ClientTemplate.java 
	 * 
	 * @param args which contains the IP address and optionally the port number of the server
	 */
	public static void main(String[] args) {
		// TODO switch this over to log4j instead of java.util.logging, it's much better
		Logger log = Logger.getLogger(ClientTemplate.class.getName());
		log.setLevel(Level.ALL);	/* record everything, let the log handler(s) filter it */
		log.info("starting main");
		/* The reason for putting this stuff here is to eventually support */
		/* refactoring to support multiple client instances */
		String hostIP = DEFAULT_HOST_ADDRESS;
		String serverName = DEFAULT_SERVER_NAME;
		int port = DEFAULT_PORT;
		
		// TODO get IP address, server name, and port from command line arguments
		if( args.length != 0) {
			log.warning("command line argument handling not supported yet, using default values");
		}
		log.config("Host Address: " + hostIP);
		log.config("Server name: " + serverName);
		log.config("Port: " + port);
		
		log.fine("Checking the security manager...");
		if( System.getSecurityManager() == null) {
			log.config("Getting a new security manager, since we didn't detect one");
			System.setSecurityManager(new RMISecurityManager());
		} else {
			log.config("User has already set up a security manager, we'll use that one");
		}
		
		log.fine("creating the ClientTemplate ojbect");
		ClientTemplate client = new ClientTemplate();
		
		log.fine("calling doClientStuff()");
		client.doClientStuff(hostIP, serverName, port);
	}
	
	void doClientStuff(String hostIP, String serverName, int port) {
		Logger log = Logger.getLogger(ClientTemplate.class.getName());
		log.fine("Running doClientStuff()");
		String argToHelloClient = "Argument to HelloClient, hash code " + this.hashCode();
		log.fine("arg to HelloClient is going to be: " + argToHelloClient);
		try {
			RemoteInterfaceTemplate server = (RemoteInterfaceTemplate) Naming.lookup("//" + hostIP + ":" + port + "/" + serverName);
			log.fine("We successfully looked up the server name: "+ server.toString());
			
			for(int i=0; i<ITERATIONS; i++) {
				log.fine("Iteration " + i);
				String retVal = server.HelloClient(argToHelloClient);
				log.fine("We called HelloClient and gotback: " + retVal);
			
				if( retVal.equals(EXPECTED_RESPONSE + argToHelloClient)) {
					log.fine("Successful response on iteration " + i);
				} else {
					System.out.println("We got back the wrong response to our RMI call");
					log.severe("wrong response from server");
					Exception badResponse = new Exception("We got a bad response to our RMI call");
					throw badResponse;
				}
			}
			System.out.println("Successful execution of " + ITERATIONS + " rmi calls");
			log.info("Successful execution of " + ITERATIONS + " rmi calls");
			
		} catch( Exception e) {
			// TODO improve the exception handling in doClientStuff, including sending StackTrace to log
			log.severe("A major error happened in the client: " + e.getMessage() );
			System.out.println("Oops! An exception occurred in the client: " + e);
			e.printStackTrace();		// Later on, send this into the log instead of the console
		}
		return;
	}
}
