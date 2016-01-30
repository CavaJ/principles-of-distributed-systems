package com.rmq.pds;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

//class to simulate xmlrpcclient
/* Additionally we have implemented serializable which can be send throuh rpc call a
   and retrieved */
public class Client implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final XmlRpcClient xmlRpcClient;
	private final XmlRpcClientConfigImpl config;
	
	//instance variable for Client's socket address
	private final String socketAddress;
	
	//master string that will be used during distributed read and write operations
	//this String will be transfered to new master once new master node has been elected
	//transferMasterString() method, initially it is empty
	private volatile StringBuffer masterString = new StringBuffer("");
	
	//boolean variable determines whether this client is a master node
	//initially false
	private boolean isMasterNode = false;
	
	//priority and status instance variables to denote client's state and position
	//for Bully Algorithm
	private boolean status; // if clients is running, status is true, 
							//if it down then the status is false
						   //which means it cannot be elected as master node
	private int priority = 0; //priority will be set once Server updateClientsList method
						  //is called, as a default priority is 0
	
	//ArrayList named addressTable to hold ip address of hosts in the net
	//this will be updated in the new join request to the network
	//in this way each client will have info about other clients
	private final ArrayList<String> addressTable = new ArrayList<String>();
	
	//each client or node has to know about the coordinator's ip in this network
	//this variable will be updated by updateCoordinator() method
	private String coordinatorIP;
	
	//instance variable to be made true, when one of the nodes
	//in the network want to start distributed read and write operations
	//start message will be propagated to all nodes in the network
	private boolean canStartDistributedReadWriteOperations = false;
	
	//Set to remember words written by client to the master string
	private final ArrayList<String> writtenWords = new ArrayList<String>();
	
	//OK count variable for counting number of OKs received by
	//receiving sites in Ricart&Agrawala algorithm, initially 0
	private volatile int OKCount = 0;
	
	//shows whether this client(node) wants to enter critical section
	//initially false this client currently does not want to access critical section
	private volatile boolean wanted = false;
	
	//shows whether the client(node) is in the critical section
	//initially false
	private volatile boolean using = false;
	
	//timestamp variable that will be used during Ricart&Agrawala algorithm
	//initially zero
	private volatile int timestamp = 0;
	
	//in Ricart and Agrawala each node maintains a request queue
	private final LinkedBlockingDeque<String> requestQueue = 
			new LinkedBlockingDeque<String>();

	//default constructor method
	public Client(String ownSocketAddress) throws MalformedURLException
	{
		//assumes serverIP as localhost and port as 8080
		this(Utils.toSocketAddress("localhost", 8080), ownSocketAddress);
	} // Client
	
	// constructor method
	public Client(String serverSocketAddress, String ownSocketAddress) 
			throws MalformedURLException {
		
		//initialize socket address of the client
		socketAddress = ownSocketAddress;
		
		//set the status to true. because client will be running
		status = true;
		
		//get the appropriate priority from Server instance
		//priority = Server.getInstance().getNextPriority();
		
		// first we need to create xml rpc client configuration
		// which contains server url, credentials , character set etc.
		config = new XmlRpcClientConfigImpl();

		//Enable extensions to transfer any kind of data
		// to enable stream mode, enableForExtenions is set as well.
		// config.setContentLengthOptional(true);
		config.setEnabledForExtensions(true);

		// enable catching and rethrowing the exceptions thrown by the server
		//config.setEnabledForExceptions(true);

		xmlRpcClient = new XmlRpcClient();
		
		config.setServerURL(new URL(Utils.toUrlString(serverSocketAddress)));

		xmlRpcClient.setConfig(config);
		
	} // Client
	
	//execute the xmlrpc request with params, this method is the wrapper for xmlrcpClient's
	//execute method
	public Object execute(String methodName, Object[] params) 
			throws XmlRpcException 
	{
		return xmlRpcClient.execute(methodName, params);
	} // execute
	
	public XmlRpcClient getXmlRpcClient() {
		return xmlRpcClient;
	} // getXmlRpcClient

	public XmlRpcClientConfigImpl getConfig() {
		return config;
	} // getConfig

	public String getSocketAddress() {
		return socketAddress;
	} // getSocketAddress
	
	//method to get the ip address of the Client
	public String getIP()
	{
		return Utils.getIPFromSocketAddress(socketAddress);
	} // getIP
	
	//method to get the endPointPort of the client
	public int getEndPointPort()
	{
		return Utils.getPortFromSocketAddress(socketAddress);
	} // getEndPointPort
	
	//update the address table with the ip addresses of the hosts in the network
	//this method will be called each time join and sign off occurs in the network
	public void updateAddressTable()
	{
		ArrayList<String> network = Server.getInstance().getNetwork();
		
		//clear the address table and update it
		addressTable.clear();
		//we will not take the first address which is the server's address
		//each client has an info about it, so we do not need to save it again.
		for (String IP: network.subList(1, network.size()))
		{
			addressTable.add(IP);
		} // addressTable
	} // updateAddressTable

	//gets the address table of this client
	public ArrayList<String> getAddressTable() {
		return addressTable;
	} // getAddressTable
	
	//for making each node in the network to know about the coordinator ip
	public void updateCoordinator()
	{
		if (Server.getInstance().getMasterNode() != null)
		{
			coordinatorIP = Server.getInstance().getMasterNode().getIP();
		} // if
		else
		{
			System.out.println("Master node has not been assigned yet, "
							+ "so coordinator ip is null");
			coordinatorIP = null;
		} // else
			
	} // updateCoordinator
	
	public String getCoordinatorIP() {
		return coordinatorIP;
	} // getCoordinatorIP

	//method to write to the master string, it returns the resulting master string 
	//back to the writer
	public String writeToMasterString(String randomEnglishWord)
	{
		
		/*StringBuilder sb = new StringBuilder(masterString);
		masterString =  sb.append(randomEnglishWord).toString();
		
		return masterString;*/
		return masterString.append(randomEnglishWord).toString();
	} // writeToMasterString
	
	
	/*//update master string
	public void setMasterString(String newString)
	{
		masterString = newString; 
	} // setMasterString
*/	
	//set the new status
	public void setStatus(boolean status) {
		this.status = status;
	} // setStatus
	
	//gets the status of the client
	public boolean isStatus() {
		return status;
	} // isStatus

	//getter method for master string
	public String getMasterString() {
		return masterString.toString();
	} // getMasterString

	//getter method for priority
	public int getPriority() {
		return priority;
	} // getPriority
	
	//this method will be called once updateClientsList is called
	//setter method for priority
	public void setPriority(int priority) {
		this.priority = priority;
	} // setPriority

	//return whether this client is master node
	public boolean isMasterNode() {
		return isMasterNode;
	} // isMasterNode

	//set this client as a master node
	public void setMasterNode(boolean isMasterNode) {
		this.isMasterNode = isMasterNode;
	} // setMasterNode
	
	//override equals method for convenience
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    
	    if (!(other instanceof Client))return false;
	    else
	    {
	    	Client otherClient = (Client) other;
	    	return getIP().equalsIgnoreCase(otherClient.getIP());
	    } // else
	} // equals
	
	// getter method
	public boolean isCanStartDistributedReadWriteOperations() {
		return canStartDistributedReadWriteOperations;
	} // isCanStartDistributedReadWriteOperations

	// setter method for the same variable
	public void setCanStartDistributedReadWriteOperations(
			boolean canStartDistributedReadWriteOperations) {
		this.canStartDistributedReadWriteOperations = canStartDistributedReadWriteOperations;
	} // setCanStartDistributedReadWriteOperations
	
	// helper method for the client to remember what has been written
	public void rememberWord(String word) {
		writtenWords.add(word);
	} // rememberWord
	
	// getter method for written words
	public ArrayList<String> getWrittenWords() 
	{
		return writtenWords;
	} // getWrittenWords
	
	//for updating OK count before entering critical section
	public int getOKCount() 
	{
		return OKCount;
	} // getOKCount
	
	//increment OK count for each "OK" message by message receivers
	public void incrementOKCount()
	{
		OKCount ++;
	} // incrementOKCount
	
	//if node is done with critical section, it resets OK count to 0
	public void resetOKCount()
	{
		OKCount = 0;
	} // resetOKCount
	
	//getter
	public boolean isWanted() {
		return wanted;
	} // isWanted

	//setter
	public void setWanted(boolean wanted) {
		this.wanted = wanted;
	} // setWanted

	public boolean isUsing() {
		return using;
	} // isUsing

	public void setUsing(boolean using) {
		this.using = using;
	} // setUsing
	
	//getter method for timestamp, will be used internally
	private int getTimestamp() {
		return timestamp;
	} // getTimestamp
	
	//getter method to get the logical clock reading
	public int getClock()
	{
		return getTimestamp();
	} // getClock
	
	//each node will update its logical clock at request sending
	public void updateClock()
	{
		timestamp ++;
	} // updateClock
	
	//update the clock depending on requester's timestamp
	public void updateClock(int requesterTimestamp)
	{
		//in lamport clock for the recieve event
		//the following happens
		//LC <- MAX( LC, LC_sender ) + 1
		timestamp = max(getTimestamp(), requesterTimestamp) + 1;
	} // updateClock
	
	//at each interation of 20 second loop all nodes will start to access critical section
	//so, clock has to be reset
	public void resetClock()
	{
		timestamp = 0;
	} // resetClock
	
	//to find maximum of sender' clock and receiver's own clock
	private int max(int recieverTimestamp, int requesterTimestamp)
	{
		return Math.max(recieverTimestamp, requesterTimestamp);
	} // max
	
	//get Request Queue
	public LinkedBlockingDeque<String> getRequestQueue() 
	{
		return requestQueue;
	} // getRequestQueue
	
	//get the request queue head
	public String getRequestQueueHead()
	{
		return requestQueue.peek();
	} // getRequestQueueHead
	
	//emptyRequestQueue
	public void emptyRequestQueue()
	{
		requestQueue.clear();
	} // emptyRequestQueue
	
	//method to for queueing the request
	private void queueRequest(String requesterIP)
	{
		requestQueue.add(requesterIP);
	} // queueRequest
	
	//toString implementation for this client, which will just print its IP
	@Override
	public String toString()
	{
		return this.getIP();
	} // toString

	// This Method will ONLY be used by ---- Centralized Mutual Exclusion Algorithm ------
	// will be used by the coordinator client to send GRANTED or DENIED to
	// requesting
	// clients who want to enter critical section
	public synchronized String receiveMessage(String message, String requesterIP) {
		// if the received message is "request"
		if (message.equalsIgnoreCase("request")) {
			
			//if critical section is empty, access has to be granted
			if (Server.getInstance().isCriticalSectionEmpty()) 
			{
				// so send "GRANTED"
				Server.getInstance().assignToCriticalSection(requesterIP);
				
				System.out.println("Critical Section after "  
						+ requesterIP + " adding: " 
						+ Server.getInstance().getCriticalSection());
				
				return "GRANTED";
			} // if
			else // someone is in the critical section
			{
				// queue the request, return DENIED
				Server.getInstance().queueRequest(requesterIP);

				// -----FOR CONVENIENCE----
				System.out.println("Adding to queue: " + requesterIP);
				System.out.println("Queue :"
						+ Server.getInstance().getWaitingQueue());
				System.out.println("Head of queue: "
						+ Server.getInstance().getWaitingQueueHead());
				// -----FOR CONVENIENCE----

				return "DENIED";
			} // else

		} // if
		else if (message.equalsIgnoreCase("release")) 
		{
			// requester is done with critical section
			// remove the requesterIP from critical section
			
			System.out.println("Critical Section before "  
					+ requesterIP + " removing: " 
					+ Server.getInstance().getCriticalSection());
			
			Server.getInstance().removeFromCriticalSection(requesterIP);
			
			// if queue is not empty, dequeue first and reply "GRANTED"
			if (!Server.getInstance().isQueueEmpty()) {
				
				//dequeue the first IP from the queue and enter it into critical
				//section
				String newRequesterIP = Server.getInstance().dequeue();
				
				//assign critical section new enterer ip
				Server.getInstance().assignToCriticalSection(newRequesterIP);
				
				System.out.println("Critical Section after "  
						+ newRequesterIP + " adding: " 
						+ Server.getInstance().getCriticalSection());
				
				//print out who is the new enterer
				System.out.println("New CS Enterer IP: " + newRequesterIP);
				
				//-------------- NEW -------------------------------------
				Client client = Server.getInstance().getClientByIP(newRequesterIP);
				// get the random English word
				String randomEnglishWord = Server.getInstance()
						.generateRandomEnglishWord();

				// method to write this generated word to
				// client's
				// buffer to check it later whether or not
				// written
				// word exists in the resulting master string
				client.rememberWord(randomEnglishWord);

				// now params will contain the client's ip who
				// will enter
				// the critical section and and the
				// randomEnglishWord
				Object[] params = new Object[] { client.getIP(),
						randomEnglishWord };

				// now enter the critical section where the word
				// will be written
				// to the coordinator's master string

				// boolean variable to hold whether
				// criticalSection entrance was OK
				//boolean isCriticalSectionSuccess = false;

				try {
					Object reply = client.execute(
							"RequestProcessor.enterCS", params);
					// reply could be like
					// "Node with ip has written some word"
					System.out.print((String) reply);
					//result.append((String) reply);
					//isCriticalSectionSuccess = true;
				} catch (XmlRpcException e) {
					System.out
							.print("XmlRpcException while calling method"
									+ " RequestProcessor.enterCS\n");
					//result.append("XmlRpcException while calling method"
							//+ " RequestProcessor.enterCS\n");
				} // catch

				//to prevent deadlock, because the same thread can
				//access the sendMessage again
				Thread tempThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						// if everything in critical section was OK
						// send "release" message to the coordinator
							
							//print out progress
							System.out.print(client.getIP() 
									+ " sending \"release\" message\n");
							//result.append(client.getIP() 
									//+ " sending \"release\" message\n");

							try {
								client.execute(
										"RequestProcessor.sendMessage",
										new Object[] { "release",
												client.getIP() });
							} catch (XmlRpcException e) {
								System.out
										.print("XmlRpcException while calling method"
												+ " RequestProcessor.sendMessage "
												+ "with message RELEASE\n");
								//result.append("XmlRpcException while calling method"
										//+ " RequestProcessor.sendMessage "
										//+ "with message RELEASE\n");
							} // catch
							
					} // run
				});
				
				//start and join
				tempThread.start();
				
				//------- NEW ---------------
				
				return "GRANTED";
			} // if
			else // queue is already empty, so reply GRANTED
			{
				return "GRANTED";
			} // else
		} // else if
			// just for convenience, we cannot get different message other than
			// "request" and "release" in the program
		else {
			return "GRANTED";
		} // else

	} // receiveMessage

	//This method will ONLY be used ----- Ricart & Agrawala Algorithm -------
	//receive request method for client which can send "OK" or "DEFERRED" messages
	//depending on a situation
	public synchronized String receiveMessage(String message, String requesterIP, int requesterTimestamp)
	{
		//upon receiving a message, this node updates its clock using the timestamp in the message
		//updateClock(requesterTimestamp);
		
		if (message.equalsIgnoreCase("request"))
		{
			updateClock(requesterTimestamp);
			
			System.out.print("\nReceiver " + getIP() + " with timestamp "
					+ getTimestamp() + " received \"request\" from requester "
					+ requesterIP);

			// if not accessing the resource and do not want to access it send
			// OK
			if (!isUsing() && !isWanted()) {
				
				//System.out.println("\nNotUSING AND NotWANTED");
				
				System.out.println(" replied \"OK\"");
				return "OK" + "," + getClock();
			} // if
			else if (isUsing()) // currently using the resource
			{
				//System.out.println("\nCurrently USING");
				
				// queue request in own queue
				queueRequest(requesterIP);

				// -----FOR CONVENIENCE----
				System.out.println("Client " + getIP()
						+ " is in critical section");
				System.out.println("Adding to " + getIP() + "'s queue: "
						+ requesterIP);
				System.out.println(getIP() + "'s Queue: " + getRequestQueue());
				System.out.println("Head of " + getIP() + "'s queue: "
						+ getRequestQueueHead());
				// -----FOR CONVENIENCE----

				System.out.println(" replied \"DEFERRED\"");
				return "DEFERRED";
			} // else if
			else if (isWanted()) // wants to access a resource too
			{
				//System.out.println("\nWANTs to Use resorce");
				
				// in extended lamport clock, machine ids are also taken into
				// account
				// LC(a) < LC(b) or [LC(a) = LC(b) and A < B)
				if (requesterTimestamp < getTimestamp()
						|| (requesterTimestamp == getTimestamp() && requesterIP
								.compareToIgnoreCase(getIP()) < 0)) 
				{
					System.out.println(" replied \"OK\"");
					return "OK" + "," + getClock();
				} // if
				else 
				{
					// queue request
					queueRequest(requesterIP);

					// -----FOR CONVENIENCE----
					System.out.println("Client " + requesterIP
							+ " has timestamp " + requesterTimestamp + " > "
							+ getTimestamp() + " timestamp" + " of receiver "
							+ getIP());
					System.out.println("Adding to " + getIP() + "'s queue: "
							+ requesterIP);
					System.out.println(getIP() + "'s Queue: "
							+ getRequestQueue());
					System.out.println("Head of " + getIP() + "'s queue: "
							+ getRequestQueueHead());
					// -----FOR CONVENIENCE----

					System.out.println(" replied \"DEFERRED\"");
					return "DEFERRED";
				}
			} // else if
			else // apparently this won't be happen
			{
				System.out.print(" replied \"null\"");
				return null;
			} // else

		} // if message is "request"
		
		//else if this node has received OK from the other node, increment ok count
		else if (message.equalsIgnoreCase("OK"))
		{
			//update clock if the OK message have been received
			updateClock(requesterTimestamp);
			
			//received OK, increment the OK count
			incrementOKCount();
			
			//System.out.println(" replied \"null\"");
			//we will not deal with return from this part
			return null;
		} // else if "OK"
		else // this else won't happen
		{
			//System.out.println(" replied \"null\"");
			return null;
		} // else
	} // receiveMessage
	
} // class Client
