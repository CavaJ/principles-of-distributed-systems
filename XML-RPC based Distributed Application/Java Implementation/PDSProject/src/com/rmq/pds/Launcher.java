package com.rmq.pds;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Scanner;

import org.apache.xmlrpc.XmlRpcException;

public class Launcher {
	
	
	public static void main(String[] args)
	{
		//get the scanner to scan standard input
		Scanner input = new Scanner(System.in);
		
		//variable that will be used interchangeably when joining new hosts
		Client client = null;
		
		//boolean variables to show the state of joining is ok and sign off is OK
		//initially false
		boolean joinOK = false;
		boolean signOffOK = false;
		
		//parameters to be sent on remote procedure call
		Object[] params = null;
		
		//get the webserver ip adress and port from console
		//basically ip address will not be used, only
		//port will be used to create a webserver,
		//normally webserver's ip will be localhost
		System.out.print("Enter the server's socket address - IP:Port => ");
		
		//variable to hold the server's socket address
		String serverSocketAddress = input.next();
		
		//while loop to ensure that correct socket address has been entered
		while (true) {
			
			//check whether the entered address is socket address
			if (Utils.isValidSocketAddress(serverSocketAddress))
			{
				//everything is OK, break the loop
				break;
			} // if
			else
			{
				System.out
				.print("Type in server 'IP:PORT' => ");
				serverSocketAddress = input.next();
			} // else
			
		} // while
		
		//create a server from factory method getInstance(int port)
		//next calls can use getInstance() to get a server singleton
		Server server = Server.getInstance(serverSocketAddress);
		
		//start the server
		server.start();
		
		//infinite while loop to request for distributed operations
		while (true) 
		{
			//code to mimic press enter to continue... message in c++
			System.out.println("Press Enter to continue...");
			try {
				System.in.read();
				showCommands();
			} catch (IOException e) {
				//shutdown server
				//close input
				//break loop
				server.stop();
				input.close();
				break;
			} // catch
			
			
			
			//get the required number to execute a task
			int command = input.nextInt();
			
			//use switch statement to execute the commands
			switch (command) 
			{
			case 1: 
			{
				if (server.isNetworkAvailable()) {
					System.out
							.print("Enter socket address of new host in form 'IP:PORT', that you want to join: ");
					String socketAddress = input.next();
					
					//while loop to ensure that correct socket address has been entered
					while (true) {
						
						//check whether the entered address is socket address
						if (Utils.isValidSocketAddress(socketAddress))
						{
							//call this method to check whether the host
							//with given ip address already exists in the network or not.
							if (checkHostEligibility(Utils
									.getIPFromSocketAddress(socketAddress)))
							{
								System.out
								.print("Host is already in the network! Type in another 'IP:PORT' => ");
								socketAddress = input.next();
							} // if
							else
							{
								//everything is OK, break the loop
								break;
							} // else
						} // if
						else
						{
							System.out
							.print("Type in another 'IP:PORT' => ");
							socketAddress = input.next();
						} // else
						
					} // while

					try {
						// create the client with server socket address and
						// its own socket address
						client = new Client(server.getSocketAddress(),
								socketAddress);
					} // try
					catch (MalformedURLException e) {
						System.err
								.println("Error Setting the server url for this client.");

						// nullify client
						client = null;
					} // catch

					// if the client is created successfully, then do rpc call
					if (client != null) {
						// we will send client's socket address as the parameter
						params = new Object[] { client.getSocketAddress() };

						// response will the ordered array of nodes in network
						Object response = null;
						
						//make convenient output
						beautifyOutput();
						
						try {
							response = client.execute(
									"RequestProcessor.joinNode", params);
							
							//print out the success message
							System.out.print((String) response);
							
							// propagating the join request to all nodes in
							// network
							try {
								//propagate message using newly joined client
								//response will contain a "success" messsage
								response = propagateMessageToAll(
										"RequestProcessor.propagateJoinRequest",
										params);
								
								/* UNUSED System.out.println("New host is joined to network "
									+ Arrays.toString(server.getNodeArray()));*/
								
								//print out whether the address table of client has been updated
								System.out.print((String) response);
								
								//checkAddressTables();
								
								//checkClients();
								
								//show the clients'size
								//System.out.println("Number of clients = " + server.getClients().get(0));
								
								//make convenient output
								beautifyOutput();
								
								//join is done, so set the variable to true
								joinOK = true;
															
							} catch (XmlRpcException e) {
								System.err
										.println("Failed to broadcast join request");
								e.printStackTrace();
							} // catch
							catch(MalformedURLException ex)
							{
								System.err
								.println("Failed to broadcast join request because of"
										+ " a malformed url of the server");
							} // catch
							
							
						} catch (XmlRpcException e) {
							System.err
									.println("Error executing xmlrpc join request, response is null");
							e.printStackTrace();
						} // catch
						
						//we execute master node election when joining is ok
						if (joinOK)
						{
							// do the master node election
							System.out.println("Master Node Election: ");

							// now send election request, 
							//election request would be sent twice, first when join
							//occurs and the second when signoff occurs
							//we will handle it carefully, by passing
							//specific paramater to a params Object array
							params = new Object[] { "join" + "," + client.getIP() };

							try {
								response = client.execute("RequestProcessor.startElection",
										params);
								if (response != null)
								{
									//print out success message
									System.out.print((String) response);
									
									//now it is time to propagate coordinator message to all nodes
									response = propagateMessageToAll
											("RequestProcessor.propagateCoordinatorMessage",
											params);
									
									//print out success message
									System.out.print((String) response);
									
									beautifyOutput();
								} // if
								
								//now make joinOK false, for this join, because
								//master election process is done
								joinOK = false;
							} catch (XmlRpcException e) {
								System.err
										.println("XmpRpcException in master election process "
												+ " or coordinator message propagation");
								// e.printStackTrace();
							} // catch
							catch(MalformedURLException ex)
							{
								System.err
								.println("Failed to broadcast coordinator message because of"
										+ " a malformed url of the server");
							} // catch
						} // if
						
					} // if client != null
					else // there is a problem with socket addresses
					{
						System.err
								.println("Try to correct servers' socket address"
										+ " or clients' socket address");

						// break the switch statement for redoing the same task
						break;
					} // else

				} // if network is available
				else 
				{
					System.out.println("Please first add your server to the network");
				} // else

				break;
			} // case for join
			case 2: 
			{
				// first check whether the server is in the network and running
				if (server.isNetworkAvailable()) {

					// check whether or not there is at least one host in the
					// network
					boolean isOneHostAvailable = false;
					
					// create the temporary client with server socket address and
					// its own socket address and do remote procedure call
					try {
						client = new Client(server.getSocketAddress(),
								Utils.getDefaultSocketAddress());
						
						Object response = client.execute("RequestProcessor.atLeastOneHostAvailable", 
								new Object[] {});
						isOneHostAvailable = (Boolean) response;
					} // try 
					catch (MalformedURLException e1) {
						System.err.println("Cannot create temporary client "
								+ "because of malformed url of the server");
					} // catch
					catch (XmlRpcException e) {
						System.err.println("XmlRpcException in executing method "
								+ "RequestProcessor.atLeastOneHostAvailable");
					} // catch
					
					if (isOneHostAvailable) 
					{
						// show the list of hosts that can be signed offed
						showNetwork();
						
						//now prompt the user for the entrance of nodeId
						System.out.print("Enter the node ID to sign it off from the network: ");
						//scan the standard input
						int nodeIdToSignOff = input.nextInt();
						
						//check whether the entered node id is server's node id
						while(nodeIdToSignOff == 0)
						{
							System.out.println("Server cannot be signed off from the network!");
							System.out.print("Enter the host node ID to sign it off from the network: ");
							nodeIdToSignOff = input.nextInt();
						} // while
						
						//do the remote procedure call to sign the node off from the network
						//we will do this by creating random client and sending sign off request
						try {
							// create the client with server socket address and
							// its own socket address
							client = new Client(server.getSocketAddress(),
									Utils.getDefaultSocketAddress());
						} // try
						catch (MalformedURLException e) {
							System.err
									.println("Error Setting the server url for this client.");

							// nullify client
							client = null;
						} // catch
						
						// if the client is created successfully, then do rpc call
						if (client != null) {
							// we will send nodeIdToSignOff as the parameter
							params = new Object[] { new Integer(nodeIdToSignOff) };

							// response will the ordered array of nodes in network
							Object response = null;
							
							//make convenient output
							beautifyOutput();
							
							try {
								response = client.execute(
										"RequestProcessor.signOffNode", params);
								
								//print out success message
								System.out.print((String) response);
								
								// propagating the signOff request to all nodes in
								// network
								try {
									//propagate message using temporary created client
									//response will contain a "success" messsage
									response = propagateMessageToAll(
											"RequestProcessor.propagateSignOffRequest",
											params);
									
									/* UNUSED System.out.println("Host is removed from network "
										+ Arrays.toString(server.getNodeArray()));*/
									
									//print out whether the address table of client has been updated
									System.out.print((String) response);
									
									//checkAddressTables();
									
									//show the clients'size
									//System.out.println("Number of clients = " + server.getClients().get(1));
									
									//make convenient output
									beautifyOutput();
									
									//if we reached here sign off is OK
									signOffOK = true;
																
								} catch (XmlRpcException e) {
									System.err
											.println("Failed to broadcast signoff request");
									e.printStackTrace();
								} // catch
								catch(MalformedURLException ex)
								{
									System.err
									.println("Failed to broadcast signoff request because of"
											+ " a malformed url of the server");
								} // catch
								
								
							} catch (XmlRpcException e) {
								System.err
										.println("Error executing xmlrpc signoff request, response is null");
								e.printStackTrace();
							} // catch
							
							//we execute master node election when signoff is ok
							if (signOffOK)
							{
								// do the master node election
								System.out.println("Master Node Election: ");

								// now send election request
								//during signoff elections process starts
								//again from first available node in the network
								
								//try
								//{
									params = new Object[] { "signOff" + "," + 
											Utils.getDefaultIP() };
								//} // try
								/*catch(IndexOutOfBoundsException ex)
								{
									System.out.println("There is no client in the network "
											+ "please join at least one");
									
									//signOff is ok, for this election which has not
									//happened because of no client in the network
									signOffOK = false;
									beautifyOutput();
									
									//break the switch for the user prompt
									break;
								} // catch
								*/
									
								try {
									response = client.execute("RequestProcessor.startElection",
											params);
									if (response != null)
									{
										//print out the message
										System.out.print((String) response);
										
										//now it is time to propagate coordinator message to all nodes
										response = propagateMessageToAll
												("RequestProcessor.propagateCoordinatorMessage",
												params);
										
										System.out.print((String) response);
										
										beautifyOutput();
									} // if
									
									//now make signOffOK false, for this signoff, because
									//master election process is done
									signOffOK = false;
								} catch (XmlRpcException e) {
									System.err
											.println("XmpRpcException in master election process "
													+ " or coordinator message propagation");
									// e.printStackTrace();
								} // catch
								catch(MalformedURLException ex)
								{
									System.err
									.println("Failed to broadcast coordinator message because of"
											+ " a malformed url of the server");
								} // catch
							} // if

						} // if client != null
						else // there is a problem with socket addresses
						{
							System.err
									.println("Try to correct servers' socket address"
											+ " or clients' socket address");

							// break the switch statement for redoing the same task
							break;
						} // else
						
					} // if there is at least one host in the network
					else 
					{
						System.out
								.println("There is no host in the network, try to join at least one");
					} // else
				} // if network availability
				else 
				{
					System.out.println("Please first add your server to the network");
				} // else
				
				break;
			} // case for signoff
			case 3:
			{
				showNetwork();
				
				//now prompt the user for the whether or not he/she wants to look at
				//address table and coordinator information of each each client
				System.out.print("Do you want see address table of each client(Y/N): ");
				
				//scan the standard input
				String prompt = input.next();
				
				//check correctness of prompt
				while( !(prompt.equalsIgnoreCase("Y") || prompt.equalsIgnoreCase("N")) )
				{
					System.out.print("Enter just Y or N: ");
					prompt = input.next();
				} // while
				
				//if prompt is YES
				if (prompt.equalsIgnoreCase("Y"))
				{
					checkAddressTables();
				} // if
				
				break;
			} // case for showing network status
			case 4: 
			{
				if(server.isNetworkAvailable())
				{
					// check whether or not there is at least one host in the
					// network
					boolean isOneHostAvailable = false;
					
					// create the temporary client with server socket address and
					// its own socket address and do remote procedure call
					try {
						client = new Client(server.getSocketAddress(),
								Utils.getDefaultSocketAddress());
						
						Object response = client.execute("RequestProcessor.atLeastOneHostAvailable", 
								new Object[] {});
						isOneHostAvailable = (Boolean) response;
					} // try 
					catch (MalformedURLException e1) {
						System.err.println("Cannot create temporary client "
								+ "because of malformed url of the server");
					} // catch
					catch (XmlRpcException e) {
						System.err.println("XmlRpcException in executing method "
								+ "RequestProcessor.atLeastOneHostAvailable");
					} // catch
					
					
					if (isOneHostAvailable)
					{
						//first propagate start message to all nodes
						Object response  = null;
						
						//propagate message to all
						//will set corresponding instance variable on the clients to true
						try {
							
							//method will return success message whether or not
							//distributed read write operations can start
							response = propagateMessageToAll("RequestProcessor.startDistributedReadWrite", 
									new Object[] { });
							String reply = (String) response;
							
							//make convenient output
							beautifyOutput();
							
							//print success message
							System.out.print(reply);
						} // try 
						catch (MalformedURLException e1) {
							System.err.println("Cannot create temporary client "
									+ "because of malformed url of the server");
						} // catch
						catch (XmlRpcException e) {
							System.err.println("XmlRpcException in executing method "
									+ "RequestProcessor.startDistributedReadWrite");
						} // catch
						
						// show the list of hosts that can be signed offed
						showAlgorithms();
						
						//now prompt the user for the entrance of algorithm id
						System.out.print("Enter synchronization algorithm ID to start: ");
						
						//scan the standard input
						int algorithmID = input.nextInt();
						
						//switch statement to achieve proper operations
						switch(algorithmID)
						{
						case 1: 
						{
							//boolean variable to hold whether or not the algorithm worked
							boolean isCeMutExOK = false;
							
							//if this case has been chosen we will start with rpc
							//call to doCeMutEx method
							// we will need a temporary client for doCeMutEx message
							// the important part is server's socket address
							try {
								client = new Client(Server.getInstance().getSocketAddress(),
										Utils.getDefaultSocketAddress());
								
								//execute doCeMutEx method on server's handler
								response = client.execute("RequestProcessor.doCeMutEx", new Object[] {});
								
								//print out what is returned
								//System.out.print((String) response);
								
								//if we have reached here centralized mutual exclusion
								//algorithm worked perfectly
								isCeMutExOK = true;
							} catch (MalformedURLException e) {
								
								System.err.println("Temporary client cannot be created because of"
										+ " malformed url of the server");
							} // catch
							catch (XmlRpcException e) {
								System.err.println("XmlRpcException in execution of remote doCeMutEx() method");
							} // catch
							
							//After process has ended all the nodes read the final string from
							//master node and write it to the screen
							//Moreover they check if all words they added to the string
							//are present in the final string. The result of this check
							//is also written to the screen.
							
							//if algorithm worked successfully, we can precede with
							//other operations
							if (isCeMutExOK)
							{
								// we will need a temporary client to 
								// execute readFinalString method
								// the important part is server's socket address
								try {
									client = new Client(Server.getInstance().getSocketAddress(),
											Utils.getDefaultSocketAddress());
									
									//execute readFinalString method on server's handler
									response = client.execute("RequestProcessor.readFinalString", new Object[] {});
									
									//print out results
									System.out.print((String) response);
									
								} catch (MalformedURLException e) {
									
									System.err.println("Temporary client cannot be created because of"
											+ " malformed url of the server");
								} // catch
								catch (XmlRpcException e) {
									System.err.println("XmlRpcException in execution of "
											+ "remote readFinalString() method");
								} // catch
							} // if algorithm worked perfectly
							
							break;
						} // case 1 (CeMutEx)
						case 2:
						{
							//boolean variable to hold whether or not the algorithm worked
							boolean isRicartAgrawalaOK = false;
							
							//if this case has been chosen we will start with rpc
							//call to doRicartAgrawala method
							// we will need a temporary client for doCeMutEx message
							// the important part is server's socket address
							try {
								client = new Client(Server.getInstance().getSocketAddress(),
										Utils.getDefaultSocketAddress());
								
								//execute doRicartAgrawala method on server's handler
								response = client.execute("RequestProcessor.doRicartAgrawala", 
										new Object[] {});
								
								//print out what is returned
								//System.out.print((String) response);
								
								//if we have reached here Ricart & Agrawala
								//algorithm worked perfectly
								isRicartAgrawalaOK = true;
							} catch (MalformedURLException e) {
								
								System.err.println("Temporary client cannot be created because of"
										+ " malformed url of the server");
							} // catch
							catch (XmlRpcException e) {
								System.err.println("XmlRpcException in execution of remote "
										+ "doRicartAgrawala() method");
							} // catch
							
							//After process has ended all the nodes read the final string from
							//master node and write it to the screen
							//Moreover they check if all words they added to the string
							//are present in the final string. The result of this check
							//is also written to the screen.
							
							//if algorithm worked successfully, we can precede with
							//other operations
							if (isRicartAgrawalaOK)
							{
								// we will need a temporary client to 
								// execute readFinalString method
								// the important part is server's socket address
								try {
									client = new Client(Server.getInstance().getSocketAddress(),
											Utils.getDefaultSocketAddress());
									
									//execute readFinalString method on server's handler
									response = client.execute("RequestProcessor.readFinalString", new Object[] {});
									
									//print out results
									System.out.print((String) response);
									
								} catch (MalformedURLException e) {
									
									System.err.println("Temporary client cannot be created because of"
											+ " malformed url of the server");
								} // catch
								catch (XmlRpcException e) {
									System.err.println("XmlRpcException in execution of "
											+ "remote readFinalString() method");
								} // catch
							} // if algorithm worked perfectly
							
							break;
						} // case 2 (Ricart&Agrawala)
						default:
						{
							System.out.println("Please enter correct algorithm id!");
							break;
						} // default
						} // switch
						
					} // if at least one host available
					else 
					{
						System.out
								.println("There is no host in the network, try to join at least one");
					} // else
					
				} // if server is available 
				else 
				{
					System.out.println("Please first add your server to the network");
				} // else
				
				break;
			} // case for starting distributed read write operations
			case 5: 
			{
				// at the end shutdown the server and close scanner
				server.stop();
				input.close();
				System.out.println("DONE !!!");
				return;
			} // case for exiting
			default: 
			{
				break;
			} // default
			
			} // switch
			
		} // while
		
	} // main
	
	//method to achieve message propagation to all nodes in the network
	public static Object propagateMessageToAll(String methodName, Object[] params) 
			throws XmlRpcException, MalformedURLException {
		
		//we will need a temporary client for propagation of the messages
		//the important part is server's socket address
		Client client = new Client(Server.getInstance().getSocketAddress(), 
				Utils.getDefaultSocketAddress());
		return client.execute(methodName, params);
	} // propagateMessageToAll
	
	//helper method to print out useful commands
	private static void showCommands()
	{
		//System.out.println();
		System.out.println("Enter 1 - to join the virtual network, you will be asked for host address and port");
		System.out.println("Enter 2 - to sign off from virtual network");
		System.out.println("Enter 3 - to show your virtual network status");
		System.out.println("Enter 4 - to start distributed read write operations, can be executed only if there is a host in the network");
		System.out.println("Enter 5 - to exit and stop the program");
	} // showCommands
	
	//print outs dashed lines to make output more convenient
	private static void beautifyOutput()
	{
		System.out.println("--------------------------------------------------------------");
	} // beautifyOutput
	
	//helper method to show the user available synchronization algorithms
	private static void showAlgorithms()
	{
		//make dashed line
		beautifyOutput();
		
		System.out.println("1 Centralized Mutual Exclusion");
		System.out.println("2 Ricart & Agrawala");
		
		//make dashed lines
		beautifyOutput();
	} // showAlgorithms
	
	//shows all the nodes in the network with specified ids
	private static void showNetwork()
	{
		// print out hosts to std output for the user to choose
		// which one to sign off
		beautifyOutput();
		
		
		// we will need a temporary client for showNetworkState message
		// the important part is server's socket address
		try {
			Client client = new Client(Server.getInstance().getSocketAddress(),
					Utils.getDefaultSocketAddress());
			
			//execute showNetworkState method on server's handler
			Object response = client.execute("RequestProcessor.showNetworkState", new Object[] {});
			
			if (response != null)
			{
				System.out.print((String) response);
			} // if
			
		} catch (MalformedURLException e) {
			
			System.err.println("Temporary client cannot be created because of"
					+ " malformed url of the server");
		} // catch
		catch (XmlRpcException e) {
			System.err.println("XmlRpcException in execution of remote showNetworkState() method");
		} // catch
		
		beautifyOutput();
	} // showNetwork
	
	
	//helper method to detect whether the address tables have been updated
	private static void checkAddressTables() {
		
		// create the temporary client with server socket address and
		// its own socket address and do remote procedure call
		try {
			Client client = new Client(Server.getInstance().getSocketAddress(),
					Utils.getDefaultSocketAddress());

			// send the given ip to request processor to check whether
			// the host with given ip already exists, if exists
			// response will false
			Object response = client.execute(
					"RequestProcessor.checkAddressTables",
					new Object[] { });
			System.out.print((String) response);
		} // try
		catch (MalformedURLException e1) {
			System.err.println("Cannot create temporary client "
					+ "because of malformed url of the server");
		} // catch
		catch (XmlRpcException e) {
			System.err.println("XmlRpcException in executing method "
					+ "RequestProcessor.checkAddressTables");
		} // catch
		
	} // checkAddressTables
	
	/*private static void checkClients()
	{
		for (Client cl : Server.getInstance().getClients()) {
			System.out.println("Client " + cl.getIP());
		} // for
	} // checkClients
	*/	
	
	//will check host's ip address's eligibility
	private static boolean checkHostEligibility(String IP)
	{
		// check whether or not there is at least one host in the
		// network
		boolean isHostWithGivenIPEligible = false;
		
		// create the temporary client with server socket address and
		// its own socket address and do remote procedure call
		try {
			Client client = new Client(Server.getInstance().getSocketAddress(),
					Utils.getDefaultSocketAddress());
			
			//send the given ip to request processor to check whether
			//the host with given ip already exists, if exists
			//response will false
			Object response = client.execute("RequestProcessor.checkHostEligibility", 
					new Object[] { IP });
			isHostWithGivenIPEligible = (Boolean) response;
		} // try 
		catch (MalformedURLException e1) {
			System.err.println("Cannot create temporary client "
					+ "because of malformed url of the server");
		} // catch
		catch (XmlRpcException e) {
			System.err.println("XmlRpcException in executing method "
					+ "RequestProcessor.checkHostEligibility");
		} // catch
		
		return isHostWithGivenIPEligible;
		
	} // checkHostEligibility
	
} // class Launcher
