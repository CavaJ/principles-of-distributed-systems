package com.rmq.pds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.xmlrpc.XmlRpcException;

public class RequestProcessor {
	
	//method to do distributed join operation on the network
	public String joinNode(String nodeSocketAddress) 
	{
		StringBuilder result = new StringBuilder("");
		/*System.out.println(Utils.logStart(0) + nodeSocketAddress + " wants to join the network");*/
		
		//NEW
		result.append(Utils.logTimestamp(0) + nodeSocketAddress + " wants to join the network" + "\n");
		//NEW
		
		//server is already in the network, it was added to the network when we initialized it.
		
		//Server.getInstance() will return the singleton that were created before
		Server.getInstance().addHost(nodeSocketAddress);
		
		//update the client list, but not each client's address table.
		//address table will be updated in join broadcast request
		Server.getInstance().updateClientList(nodeSocketAddress);
		
		//method to set each client isMasterNode variable to zero
		//because the election process will start again
		//---- NEW
		Server.getInstance().resetNodesForCoordinatorElection();
		//---- NEW
		
		//helper method which help to enumerate clients' priorities
		//in the case of 1, 2, 4 to 1, 2, 3 because 3rd node has been signed off
		//this will also be called in signOffNode method
		Server.getInstance().enumerateClientsPriorities();
		
		//show the state-of-art of network state
		/*System.out.println(Utils.logStart(0) + Arrays.toString(Server.getInstance().getNodeArray()));*/
		// NEW
		result.append(Utils.logTimestamp(0)
				+ Arrays.toString(Server.getInstance().getNodeArray()) + "\n");
		// NEW
		
		//and return node ips as a string delimited by commas
		/*return getNodesString();*/
		return result.toString();
	} // joinNode
	
	//method to do distributed sign off operation from the network
	public String signOffNode(Integer nodeID)
	{
		StringBuilder result = new StringBuilder("");
		
		//nodeId is actually the index of the nodes to be removed from
		//arraylist network which resides in Server class
		String nodeIPToBeSignedOff = Server.getInstance().getNetwork().get(nodeID);
		
		//print out which node wants to be signed off
		/*System.out.println(Utils.logStart(0) + nodeIPToBeSignedOff 
							+ " wants to sign off  from the network");*/
		
		//NEW
		result.append(Utils.logTimestamp(0) + nodeIPToBeSignedOff 
				+ " wants to sign off  from the network" + "\n");
		//NEW
		
		//Server.getInstance() will return the singleton that were created before
		//removeHost will remove the host from network with given ip
		Server.getInstance().removeHost(nodeIPToBeSignedOff);
		
		//now it is time to remove the client with given ip from clients list
		Server.getInstance().removeClientByIP(nodeIPToBeSignedOff);
		
		// method to set each client isMasterNode variable to zero
		// because the election process will start again
		// ---- NEW
		Server.getInstance().resetNodesForCoordinatorElection();
		// ---- NEW
		
		//if all clients has been signed off from the network reset the next priority
		//for new joining clients in the future
		if (Server.getInstance().getClients().size() == 0)
		{
			Server.getInstance().resetNextPriority();
		} // if
		
		// helper method which help to enumerate clients' priorities
		// in the case of 1, 2, 4 to 1, 2, 3 because 3rd node has been signed off
		Server.getInstance().enumerateClientsPriorities();
		
		//show the state-of-art of network state
		/*System.out.println(Utils.logStart(0) + Arrays.toString(Server.getInstance().getNodeArray()));*/
		
		//NEW
		result.append(Utils.logTimestamp(0) + Arrays.toString(Server.getInstance().getNodeArray())
				+ "\n");
		//NEW
		
		//and return node ips as a string delimited by commas
		/*return getNodesString();*/
		return result.toString();
		
	} // signOffNode
	
	//this variables will be used in election process
	private int[] clientsPriorities;
	private boolean[] clientsStatuses;
	private int coordinatorNumber;
	private int numberOfClients;
	private String resultStringToBeReturned;
	
	//distributed method to start election on current nodes of the network
	//it throws index out of bounds exception, when we start election
	//that there is no node to be elected, it is caught in the client side
	public String startElection(String actionStarterIPBundle) throws IndexOutOfBoundsException
	{
		resultStringToBeReturned = "";
		
		Client starterClient = null;
		
		//split the string 
		String[] components = actionStarterIPBundle.split(",");
		String action = components[0];
		String starterIP = components[1];
		
		try {
			// we will handle master election in two ways
			if (action.equalsIgnoreCase("join")) {
				starterClient = Server.getInstance().getClientByIP(starterIP);
			} // if
			else if (action.equalsIgnoreCase("signOff")) {
				starterIP = Server.getInstance().getFirstRemainingClient()
						.getIP();
				starterClient = Server.getInstance().getClientByIP(starterIP);
			} // else
		} catch (IndexOutOfBoundsException ex) {
			//we could get IndexOutOfBoundsException when we call getFirstRemainingClient
			/*System.out.println("There is no client in the network "
					+ "please join at least one");*/
			
			resultStringToBeReturned += "There is no client in the network "
					+ "please join at least one" + "\n";
		} // catch
		
		//store clientsPriorities and statuses in the array
		clientsPriorities = new int[Server.getInstance().getClients().size()];
		clientsStatuses = new boolean[Server.getInstance().getClients().size()];
		
		numberOfClients = Server.getInstance().getClients().size();
		
		int index = 0;
		//now assign clientsPriorities and clientStatuses
		for (Client client : Server.getInstance().getClients())
		{
			clientsPriorities[index] = client.getPriority();
			clientsStatuses[index] = client.isStatus();
			index ++;
		} // for
		
		//if starterClient is not null
		if (starterClient != null)
		{
			//System.out.println("starter's priority " + starterClient.getPriority());
			//normally priority is the same as the index + 1 of client clients arraylist
			elect(starterClient.getPriority());
			
			//set this coordinator as master node
			//coordinator number starts from 1, so we need index it to start from zero
			Client masterClient = Server.getInstance().getClients().get(coordinatorNumber - 1);
			masterClient.setMasterNode(true);
		
			//NEW
			//System.out.println("Final coordinator is node " + coordinatorNumber 
								//+ " with IP " 
								//+ masterClient.getIP());
			resultStringToBeReturned += "Final coordinator is node " + coordinatorNumber 
					+ " with IP " 
					+ masterClient.getIP() + "\n";
			//NEW
			
			//will return success message with coordinator's nodeId and ip address
			/*return "Final coordinator is node " + coordinatorNumber 
					+ " with IP " 
					+ masterClient.getIP() + "\n";*/
			return resultStringToBeReturned;
		} //if
		else
		{
			/*System.out.println("There is no starter client with given"
					+ " properties to start master election");*/
			resultStringToBeReturned += "There is no starter client with given"
					+ " properties to start master election" + "\n";
			/*return null;*/
			
			return resultStringToBeReturned;
		} // else
		
	} // startElection
	
	//helper method on election
	//this method implements bully algorithm
	//election will happen recursively
	private void elect(int elector)
	{
		elector = elector - 1;
		coordinatorNumber = elector + 1;
		
		for(int i = 0; i < numberOfClients; i ++)
        {
            if(clientsPriorities[elector] < clientsPriorities[i])
            {
               /* System.out.println("Election message is sent from node " + (elector + 1)
                					+ " to node " + (i + 1));*/
            	
            	resultStringToBeReturned 
            		+= "Election message is sent from node " + (elector + 1)
        					+ " to node " + (i + 1) + "\n";
            	
                if(clientsStatuses[i])
                    elect(i + 1);
            } // if
        } // for
	} // elect
	
	//private helper method to get nodes string after each join and signoff operation
	@SuppressWarnings("unused")
	private String getNodesString() {
		// return a result as a lot of ip:port delimited by comma
		// do this by creating an empty string builder and appending nodes to it
		StringBuilder result = new StringBuilder("");

		// update the string for each node
		for (String node : Server.getInstance().getNodeArray()) {
			result.append("," + node);
		} // for

		// we will not take the first element of string because it is the server
		// we only want to return nodes' ip addresses
		return result.substring(1, result.length());
	} // getNodesString
	
	//this method will be used for propagation of the messages
	public String propagateJoinRequest(String nodeSocketAddress)
	{
		StringBuilder result = new StringBuilder("");
		
		//only the broadcasted clients will update their address table
		//not the joined client
		
		/*for (Client client : Server.getInstance().getBroadcastClients())*/
		for (Client client : Server.getInstance().getClients())
		{
			client.updateAddressTable();
		} // for
		
		//NEW
		//System.out.println("New host is joined to network "
				//+ Arrays.toString(Server.getInstance().getNodeArray()));
		
		result.append("New host is joined to network "
				+ Arrays.toString(Server.getInstance().getNodeArray()) + "\n");
		
		//System.out.println("Join message propagated and address table of each client updated");
		result.append("Join message propagated and address table of each client updated"
				+ "\n");
		//NEW
		
		//by the address table client knows about other clients
		/*return "Join message propagated and address table of each client updated";*/
		return result.toString();
	} // propgateJoinRequest
	
	// this method will be used for propagation of the messages
	public String propagateSignOffRequest(Integer nodeId) 
	{
		StringBuilder result = new StringBuilder("");
		
		// all remaining clients will update their address table
		for (Client client : Server.getInstance().getClients()) {
			client.updateAddressTable();
		} // for

		//NEW
		//System.out.println("Host is removed from network "
				//+ Arrays.toString(Server.getInstance().getNodeArray()));
		
		result.append("Host is removed from network "
				+ Arrays.toString(Server.getInstance().getNodeArray()) + "\n");
		
		//System.out.println("SignOff message propagated and address table of each client updated");
		result.append("SignOff message propagated and address table of each client updated"
				+ "\n");
		//NEW
		
		// by the address table client knows about other clients
		/*return "SignOff message propagated and address table of each client updated";*/
		return result.toString();
	} // propagateSignOffRequest
	
	//method to propagate coordinator message on the network
	//this method will update each client's coordinatorIP variable
	public String propagateCoordinatorMessage(String starterIP)
	{
		StringBuilder result = new StringBuilder("");
		
		//boolean variable to ensure whether the election has happened
		//if happened we will have at least one client in the list
		boolean isElectionHappened = false;
		
		//for each client update their coordinator ips
		for (Client client : Server.getInstance().getClients())
		{
			client.updateCoordinator();
			isElectionHappened = true;
		} // for
		
		//NEW
		//System.out.println("Coordinator message propagated and now each node knows about the coordinator");
		
		//check whether election has happened print out the coordinator success message
		if(isElectionHappened) {
			result.append("Coordinator message propagated and now each node knows about the new coordinator"
					+ "\n");
		} // if
		//New
		
		/*return "Coordinator message propagated and now each node knows about the coordinator";*/
		return result.toString();
	} // propagateCoordinatorMessage
	
	//method to show the machines in the network
	public String showNetworkState() 
	{
		StringBuilder result = new StringBuilder("");
		
		int nodeId = 0;
		String[] nodes = Server.getInstance().getNodeArray();

		for (String nodeIP : nodes) {
			if (nodeId == 0) {
				//System.out.println("Server " + nodeId + " - " + nodeIP);
				result.append("Server " + nodeId + " - " + nodeIP + "\n");
				nodeId++;
			} // if
			else {
				//System.out.println("Host " + nodeId + " - " + nodeIP);
				
				//NEW
				//append also "coordinator" string to host which is the coordinator
				if (Server.getInstance().getMasterNode().getIP().equalsIgnoreCase(nodeIP))
				{
					result.append("Host " + nodeId + " - " + nodeIP + " [COORDINATOR]" + "\n");
				} // if
				else
				{
					result.append("Host " + nodeId + " - " + nodeIP + "\n");
				} // else
				//NEW
				
				/*result.append("Host " + nodeId + " - " + nodeIP + "\n");*/
				nodeId++;
			} // else
		} // for
		
		return result.toString();
	} // showNetworkState
	
	//to check whether there is at least one host in the network
	public Boolean atLeastOneHostAvailable()
	{
		return Server.getInstance().atLeastOneHostAvailable();
	} // atLeastOneHostAvailable
	
	//to check whether the host with given ip is eligible to join to the network
	public Boolean checkHostEligibility(String IP)
	{
		return Server.getInstance().checkHostEligibility(IP);
	} // checkHostEligibility
	
	//method to propagate start message for distributed read write operations
	public String startDistributedReadWrite()
	{
		//enumerate clients to set the instance variables to true
		for (Client client : Server.getInstance().getClients())
		{
			client.setCanStartDistributedReadWriteOperations(true);
		} // for
		
		return "Start message propagated and nodes can start distributed read"
				+ " and write operations\n";
	} // startDistributedReadWrite
	
	//method to achieve Centralized Mutual Exclusion Algorithm
	public String doCeMutEx()
	{	
		StringBuilder result = new StringBuilder("");
		
		//random instance to generate random amount of waiting time
		Random rand = new Random();
		
		long startTime = System.currentTimeMillis();
		//process takes 20 seconds
		//during this time all nodes in the network will do the following:
		//1.Wait Random amount of time
		//2.Read the String variable from the master node
		//3.Append some English word to this string
		//4.Write updated string to the master node
		
		int timeStamp = 0;
		
		while(System.currentTimeMillis() - startTime <= 20 * 1000) 
		{
			//sleep a little bit
			int timeToSleep = rand.nextInt(2) + 1;
			
			try 
			{
				result.append(Utils.logTimestamp(timeStamp) + "sleep for "
						+ timeToSleep + " seconds.\n");
				System.out.print(Utils.logTimestamp(timeStamp) + "sleep for "
						+ timeToSleep + " seconds.\n");
				Thread.sleep(timeToSleep * 1000);
			} // try 
			catch (InterruptedException e) {
				System.out.println("Interrupted exception on calling Thread.sleep method");
			} // catch
			
			//get the clients, randomize the sequence, make a shallow copy
			//original client arraylist does not change
			ArrayList<Client> clientList = Server.getInstance().getShallowCopyClients();
			
			//shuffle
			//Collections.shuffle(clientList);
			Client[] clients = clientList.toArray(new Client[] {});
			
			//get the clients, execute them concurrently
			//Client[] clients = Server.getInstance().getClientsArray();
			Thread[] threads = new Thread[clients.length];
			
			// for each client
			for (int i = 0; i < clients.length; i++) 
			{

				// the variable to suppress warning of "index must be final"
				final int index = i;

				threads[index] = new Thread(new Runnable() {

					@Override
					public void run() {

						// in centralized approach every node will
						// make a connection with a coordinator
						// client can send a "request" or "release" message to
						// the coordinator
						// params will contain the message and who sent the
						// message
						Object[] params = new Object[] { "request",
								clients[index].getIP() };

						// do a message send to the server which will pass it to
						// the coordinator
						// server plays a role of middleware here

						// boolean variable to achieve consistency in the code
						boolean isMessageSendOk = false;

						// reply will contains the string GRANTED or DENIED
						Object reply = null;

						try {
							reply = clients[index].execute(
									"RequestProcessor.sendMessage", params);
							isMessageSendOk = true;
						} catch (XmlRpcException e) {
							System.out
									.print("XmlRpcException in executing the "
											+ "RequestProcessor.sendMessage method\n");
							result.append("XmlRpcException in executing the "
									+ "RequestProcessor.sendMessage method\n");
						} // catch

						// if "request" message has been submitted successfully
						if (isMessageSendOk) {
							String replyString = (String) reply;

							// if reply was GRANTED
							if (replyString.equalsIgnoreCase("GRANTED")) {
								// get the random English word
								String randomEnglishWord = Server.getInstance()
										.generateRandomEnglishWord();

								// method to write this generated word to
								// client's
								// buffer to check it later whether or not
								// written
								// word exists in the resulting master string
								clients[index].rememberWord(randomEnglishWord);

								// now params will contain the client's ip who
								// will enter
								// the critical section and and the
								// randomEnglishWord
								params = new Object[] { clients[index].getIP(),
										randomEnglishWord };

								// now enter the critical section where the word
								// will be written
								// to the coordinator's master string

								// boolean variable to hold whether
								// criticalSection entrance was OK
								boolean isCriticalSectionSuccess = false;

								try {
									reply = clients[index].execute(
											"RequestProcessor.enterCS", params);
									// reply could be like
									// "Node with ip has written some word"
									System.out.print((String) reply);
									result.append((String) reply);
									isCriticalSectionSuccess = true;
								} catch (XmlRpcException e) {
									System.out
											.print("XmlRpcException while calling method"
													+ " RequestProcessor.enterCS\n");
									result.append("XmlRpcException while calling method"
											+ " RequestProcessor.enterCS\n");
								} // catch

								// if everything in critical section was OK
								// send "release" message to the coordinator
								if (isCriticalSectionSuccess) {
									// send "release"
									params = new Object[] { "release",
											clients[index].getIP() };
									
									System.out.print(clients[index].getIP() 
											+ " sending \"release\" message\n");
									result.append(clients[index].getIP() 
											+ " sending \"release\" message\n");

									try {
										clients[index].execute(
												"RequestProcessor.sendMessage",
												params);
									} catch (XmlRpcException e) {
										System.out
												.print("XmlRpcException while calling method"
														+ " RequestProcessor.sendMessage "
														+ "with message RELEASE\n");
										result.append("XmlRpcException while calling method"
												+ " RequestProcessor.sendMessage "
												+ "with message RELEASE\n");
									} // catch

								} // if critical section has been completed
									// successfully

							} // if access granted

						} // if "request" message has been sent successfully

					} // run
				});

			} // for each client
			
			//now start the threads concurrently
			for (Thread thread : threads)
			{
				thread.start();
			} // for
			
			// Now everything's running - join all the threads
			// started all the threads you want to run in parallel,
			// then call join on them all
			// this will prevent threads run sequentially.
			for (Thread thread : threads) {
			     try {
					thread.join();
				} catch (InterruptedException e) {
					System.out.println("Interrupted exception in join call");
				} // catch
			} // for
			
			timeStamp ++;
			
		} // while
		
		// sleep here for 10 seconds before all threads finish the job
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			System.out.println("Interrupted Exception in Thread.sleep call");
		} // catch
		
		//return the result string back
		return result.toString();
		
	} // doCeMutEx
	
	//method to achieve Ricart & Agrawala Algorithm
	public String doRicartAgrawala() 
	{	
		StringBuilder result = new StringBuilder("");

		// random instance to generate random amount of waiting time
		Random rand = new Random();

		long startTime = System.currentTimeMillis();
		// process takes 20 seconds
		// during this time all nodes in the network will do the following:
		// 1.Wait Random amount of time
		// 2.Read the String variable from the master node
		// 3.Append some English word to this string
		// 4.Write updated string to the master node

		int timeStamp = 0;
		
		while (System.currentTimeMillis() - startTime <= 20 * 1000) {
			
			// sleep a little bit
			int timeToSleep = rand.nextInt(2) + 1;

			try {
				result.append(Utils.logTimestamp(timeStamp) + "sleep for "
						+ timeToSleep + " seconds.\n");
				System.out.print(Utils.logTimestamp(timeStamp) + "sleep for "
						+ timeToSleep + " seconds.\n");
				Thread.sleep(timeToSleep * 1000);
			} // try
			catch (InterruptedException e) {
				System.out
						.println("Interrupted exception on calling Thread.sleep method");
			} // catch

			// get the clients, randomize the sequence, make a shallow copy
			// original client arraylist does not change
			ArrayList<Client> clientList = Server.getInstance()
					.getShallowCopyClients();

			// shuffle
			// Collections.shuffle(clientList);
			Client[] clients = clientList.toArray(new Client[] {});

			// get the clients, execute them concurrently
			Thread[] threads = new Thread[clients.length];
			
			// performed by each client at initialization
			//that state_Pi:= RELEASED
			for (Client client : clientList)
			{
				client.setUsing(false);
				client.setWanted(false);
				client.resetOKCount();
				client.resetClock();
				client.emptyRequestQueue();
			} // for

			for (int i = 0; i < clients.length; i++) {

				//to suppress warnings
				final int index = i;

				threads[index] = new Thread(new Runnable() {

					@Override
					public void run() {

						//get clients other than this client to send requests to them
						Client[] clientsOtherThanThis = Server.getInstance()
								.getClientsArrayOtherThan(clients[index]);
						
						//set the state of this client to wanted, because
						//it wants to access critical section
						clients[index].setWanted(true);
						
						//and before submitting request, the client must update its
						//logical clock, so
						//update clock, which will increment default timestamp
						//basically Ricart&Agrawala is using extended Lamport clock
						//in extended lamport clock, for each send event, the node
						//has to update its clock (increment timestamp)
						clients[index].updateClock();
						
						//reply Object back from xml rpc
						Object reply = null;
						
						//now send request to each client other than this client
						for (Client client : clientsOtherThanThis)
						{
							//Clock does not need to be 
							//updated here for each send request
							
							boolean isRequestSendingOK = false;
							
							//now send message with message,
							//requesterIP, requesterTimestamp and
							//receiverIP
							try {
								
								//print about the action to retrace the process
								System.out.print("Client " + clients[index].getIP()
										+ " with timeStamp " + clients[index].getClock()
										+ " sending \"request\" to client " + client.getIP() 
										+ "\n");
								result.append("Client " + clients[index].getIP()
										+ " with timeStamp " + clients[index].getClock()
										+ " sending \"request\" to client " + client.getIP() 
										+ "\n");
								
								reply = clients[index].execute("RequestProcessor.sendMessage",
										new Object[] 
												{ 
													"request",
													clients[index].getIP(),
													clients[index].getClock(),
													client.getIP()
												});
								
								//if we have reached here request sending has been successful
								isRequestSendingOK = true;
							} catch (XmlRpcException e) {
								System.out.print("XmlRpcException while executing the "
										+ "RequestProcessor.sendMessage method for"
										+ " each client other than this client\n");
								result.append("XmlRpcException while executing the "
										+ "RequestProcessor.sendMessage method for"
										+ " each client other than this client\n");
							} // catch
							
							//if request sending is OK
							if(isRequestSendingOK)
							{
								//cast reply to string
								String replyString = (String) reply;
								
								//---- NEW----
								
								//replystring could be like OK,1 or deferred
								//fist we need to check that replystring contains ,
								if (replyString.contains(","))
								{
									 //now split and get OK sender's timestamp
									String[] splitComponents = replyString.split(",");
									
									if (splitComponents[0].equalsIgnoreCase("OK"))
									{
										//update the logical clock, when received OK
										//from the other node, because it is receive event
										clients[index]
												.updateClock(Integer.parseInt(splitComponents[1]));
									
										//increment the OK count for this client
										clients[index].incrementOKCount();
									} // if got "OK"
									
								} // if there is comma in reply
								
								//--------  NEW -------
								
								/*if (replyString.equalsIgnoreCase("OK"))
								{
									//if the node receives "OK" message does it needs to update its logical clock?
								
								
									//increment the OK count for this client
									clients[index].incrementOKCount();
								} // if got "OK"
								*/
								
							} // if request sending is OK
							
						} // for
						
						//so, of number of OKCount is the same as number of clients
						//other than this. In other words, if received "OK" from
						//all clients that this client has sent the request to
						
						//------ NEW ------
						//while(clients[index].getOKCount() != clientsOtherThanThis.length)
						//{
							//wait
						//} // while
						//-----NEW---------
						
						if (clients[index].getOKCount() == clientsOtherThanThis.length)
						{
							//get lock on Server instance to enter critical section
							//this will ensure one node will be in critical section
							//synchronized (Server.getInstance()) {
								// now this client can enter the critical
								// section
								// set the using flag for this client
								clients[index].setUsing(true);

								// --------- NEW --------------
								// assign critical section new enterer ip
								Server.getInstance().assignToCriticalSection(
										clients[index].getIP());

								/*System.out.println("Critical Section after "
										+ clients[index].getIP()
										+ " adding: "
										+ Server.getInstance()
												.getCriticalSection());*/
								
								// ---------- NEW --------------

								// get the random English word
								String randomEnglishWord = Server.getInstance()
										.generateRandomEnglishWord();

								// method to write this generated word to
								// client's
								// buffer to check it later whether or not
								// written
								// word exists in the resulting master string
								clients[index].rememberWord(randomEnglishWord);

								// now enter the critical section where the word
								// will be written
								// to the coordinator's master string

								// boolean variable to hold whether
								// criticalSection entrance was OK
								boolean isCriticalSectionSuccess = false;

								try {

									// params will contain the client's ip who
									// will enter
									// the critical section and and the
									// randomEnglishWord
									reply = clients[index].execute(
											"RequestProcessor.enterCS",
											new Object[] {
													clients[index].getIP(),
													randomEnglishWord });
									// reply could be like
									// "Node with ip has written some word"
									System.out.print((String) reply);
									result.append((String) reply);

									// if we have reached here critical section
									// completed successfully
									isCriticalSectionSuccess = true;
								} catch (XmlRpcException e) {
									System.out
											.print("XmlRpcException while calling method"
													+ " RequestProcessor.enterCS\n");
									result.append("XmlRpcException while calling method"
											+ " RequestProcessor.enterCS\n");
								} // catch

								// if everything in critical section
								// has been completed successfully
								if (isCriticalSectionSuccess) {
									// update clock when sending OK
									clients[index].updateClock();

									int oksSent = 0;
									int numberOfNodesOkToBeSent = clients[index]
											.getRequestQueue().size();
									// get the queue, send OK to all processes
									// in own queue
									// and empty queue.
									for (String IP : clients[index]
											.getRequestQueue()) 
									{
										// Client client =
										// Server.getInstance().getClientByIP(IP);
										// client.incrementOKCount();

										try {

											// print about the action to retrace
											// the process
											System.out.print("Client "
													+ clients[index].getIP()
													+ " with timeStamp "
													+ clients[index].getClock()
													+ " sending \"OK\" to client "
													+ IP + "\n");
											result.append("Client "
													+ clients[index].getIP()
													+ " with timeStamp "
													+ clients[index].getClock()
													+ " sending \"OK\" to client "
													+ IP + "\n");

											clients[index]
													.execute(
															"RequestProcessor.sendMessage",
															new Object[] {
																	"OK",
																	clients[index]
																			.getIP(),
																	clients[index]
																			.getClock(),
																	IP });

											// if we have reached here request
											// sending has been successful
											oksSent++;
										} catch (XmlRpcException e) {
											System.out
													.print("XmlRpcException while executing the "
															+ "RequestProcessor.sendMessage method for"
															+ " each client other than this client\n");
											result.append("XmlRpcException while executing the "
													+ "RequestProcessor.sendMessage method for"
													+ " each client other than this client\n");
										} // catch

									} // for each node in request queue of this
										// node

									// if all oks sent successfully
									if (oksSent == numberOfNodesOkToBeSent) {
										// requester is done with critical
										// section
										// remove the requesterIP from critical
										// section

										
										/*System.out.println("Critical Section before "
												+ clients[index].getIP()
												+ " removing: "
												+ Server.getInstance()
														.getCriticalSection());*/

										Server.getInstance()
												.removeFromCriticalSection(
														clients[index].getIP());

										// now empty queue, set using and wanted
										// flags to false
										// and resetOKCount to 0.
										clients[index].emptyRequestQueue();
										clients[index].setUsing(false);
										clients[index].setWanted(false);
										clients[index].resetOKCount();
									} // if

								} // if critical section success

								// } // if critical section is empty
								
							//} // lock Server.getInstance
							
						} // if received OK from all nodes

					} // run
				});

			} // for each client

			// now start the threads concurrently
			for (Thread thread : threads) {
				thread.start();
			} // for
			
			// Now everything's running - join all the threads
			// started all the threads you want to run in parallel,
			// then call join on them all
			// this will prevent threads run sequentially.
			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					System.out.println("Interrupted exception in join call");
				} // catch
			} // for
			
			//increment timeStamp
			timeStamp ++;
			
		} // while	
		
		//return the result string back
		return result.toString();
	} // doRicartAgrawala
	
	//helper method for message send to the coordinator
	//Will be used by ----- Centralized Mutual Exclusion Algorithm ------
	public synchronized String sendMessage(String message, String requesterIP)
	{
		//call the receiveMessage method in coordinator
		return Server.getInstance().getMasterNode().receiveMessage(message, requesterIP);
	} // sendMessage
	
	//helper method for request sending to all nodes other than this
	//Will be used by ------- Ricart & Agrawala Algorithm -------
	public synchronized String sendMessage(String message, String requesterIP, 
			int requesterTimestamp, String receiverIP)
	{
		Client client = Server.getInstance().getClientByIP(receiverIP);
		return client.receiveMessage(message, requesterIP, requesterTimestamp);
	} // sendRequest
	
	//helper method to simulate the entrance to critical section
	//where will write the word to master string of coordinator
	public synchronized String enterCS(String entererIP, String word)
	{
		String newMasterStirng = Server.getInstance().getMasterNode().writeToMasterString(word);
		return "Node " + entererIP + " has written " + "\"" + word + "\""
				+ " to master string: " + newMasterStirng + "\n";
	} // enterCS
	
	//helper method that each node will read final string at the end and write it to the screen
	//will also check the words they have added exist in the final string
	public String readFinalString()
	{
		StringBuilder result = new StringBuilder("");
		String finalString = null;
		
		//each client will read the final string from master node and 
		//check whether their words that have been written exists in it
		for (Client client : Server.getInstance().getClients())
		{
			//make convenient output first
			System.out.print(getConvenientOutput());
			result.append(getConvenientOutput());
			
			//first get final master string from master node
			finalString = Server.getInstance().getMasterNode().getMasterString();
			
			//output what has been read
			System.out.print("Client " + client.getIP() + " read final string: "
					+ finalString + "\n");
			result.append("Client " + client.getIP() + " read final string: "
					+ finalString + "\n");
			
			//now output what client has been written to final string
			System.out.print("Client " + client.getIP() + " has written the following words:\n");
			result.append("Client " + client.getIP() + " has written the following words:\n");
			
			int wordId = 1;
			//enumerate words
			for (String word : client.getWrittenWords())
			{
				System.out.print(wordId + " " + "\"" + word + "\"" 
						+ " exists in final string: " + finalString.contains(word) + "\n");
				result.append(wordId + " " + "\"" + word + "\"" 
						+ " exists in final string: " + finalString.contains(word) + "\n");
				wordId ++;
			} // for
			
			//make convenient output at the end too...
			System.out.print(getConvenientOutput());
			result.append(getConvenientOutput());
		} // for
		
		return result.toString();
	} // readFinalString
	
	//helper method for output
	private String getConvenientOutput()
	{
		return "--------------------------------------------------------------\n";
	} // getConvenientOutput
	
	public String checkAddressTables()
	{
		StringBuilder sb = new StringBuilder("");
		
		// check whether the clients' address table has been updated
		for (Client cl : Server.getInstance().getClients()) {
			
			sb.append(getConvenientOutput());
			sb.append("Client " + cl.getIP() + " address table:\n");

			for (String address : cl.getAddressTable()) {
				
				//append each client knows about coordinator
				//knows about coordinator
				if (cl.getCoordinatorIP().equalsIgnoreCase(address))
				{
					sb.append(address + " *COORDINATOR*" + "\n");
				} // if
				else
				{
					sb.append(address + "\n");
				} // else
			} // for
			
			sb.append(getConvenientOutput());
		} // for
		
		return sb.toString();
		
	} // checkAddressTables
	
} // class RequestProcessor
