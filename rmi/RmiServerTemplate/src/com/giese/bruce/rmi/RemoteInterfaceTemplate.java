package com.giese.bruce.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/*
 * This file contains a working template for the interface for remote acess from a client to a server.
 */

// TODO add versioning to the interface

public interface RemoteInterfaceTemplate extends Remote {
	String HelloClient(String TemplateArg) throws RemoteException;
}
