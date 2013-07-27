package com.giese.bruce.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO change the client and server policy files so they don't have the same name.

/**
 * This file contains a template for a server application which receives rmi client requests.
 * See the client application file for more details.
 * <p>
 * Note that you need to set some things on both the client and the server in order for this
 * to work.  On the server, you need to do the following:
 *    1) Create a policy file and make sure it's in the classpath (let's call it network.policy).
 *       In this template, I've placed it in the project root directory.
 *    2) Set the java VM arguments to include -Djava.security.policy=network.policy
 *    3) Set the java VM arguments to include -Djava.rmi.server.hostname= the host IP address or net addr.
 *    4) For logging, create a properties file and make sure it's in the class path
 *       (let's call it logging.properties)
 *    5) For logging, set the java VM arguments to include -Djava.util.logging.config.file="logging.properties"
 *    6) Before running the program, you need to go into the folder with the .class files...
 *       this might be a location such as /workspace/RmiServerTemplate/bin
 *       (Note that it failed when it put it into /workspace/RmiServerTemplate/bin/com/giese/bruce/rmi)
 *       ...and run rmiregistry in the background ("&" in linux)
 *    
 * @author		Bruce Giese
 * @version		0.1
 *
 */
public class RemoteInterfaceTemplateImpl extends UnicastRemoteObject implements
		RemoteInterfaceTemplate {
	static final long serialVersionUID = 1;
	static final String EXPECTED_RESPONSE = "The server successfully received: ";
	static final String SERVER_NAME = "TemplateServer";
	
	public RemoteInterfaceTemplateImpl() throws RemoteException {
		super();	/* this is completely unnecessary */
	}

	@Override
	public String HelloClient(String TemplateArg) throws RemoteException {
		Logger log = Logger.getLogger(RemoteInterfaceTemplateImpl.class.getName());
		log.fine(EXPECTED_RESPONSE + TemplateArg);
		return EXPECTED_RESPONSE + TemplateArg;
	}

	public static void main(String[] args) {
		Logger log = Logger.getLogger(RemoteInterfaceTemplateImpl.class.getName());
		log.setLevel(Level.ALL);
		
		log.info("starting main");
		
		if( System.getSecurityManager() == null) {
			log.config("Getting a new security manager, since we didn't detect one");
			System.setSecurityManager(new RMISecurityManager());
		} else {
			log.config("User has already set up a security manager, we'll use that one");
		}
		
		try {
			log.fine("Getting a server instance, which can throw a remote exception");
			RemoteInterfaceTemplate server = new RemoteInterfaceTemplateImpl();
			
			log.fine("binding the name");
			Naming.rebind(SERVER_NAME, server);
			
			log.fine("Done setting up the server");

		} catch(RemoteException re) {
			log.severe("Got a RemoteException: " + re);
			// TODO Send the stack trace out to the log instead of the console
			re.printStackTrace();
		} catch(MalformedURLException urle) {
			log.severe("Got a Malformed URL Exception: " + urle);
			// TODO Send the stack trace out to the log instead of the console
			urle.printStackTrace();
		} catch(Exception e) {
			log.severe("We got some unexpected exception: " + e);
			// TODO Send the stack trace out to the log instead of the console
			e.printStackTrace();
		}
	}
}
