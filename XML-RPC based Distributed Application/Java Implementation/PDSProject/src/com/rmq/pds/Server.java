package com.rmq.pds;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

//class to simulate minimal http webserver using xmlrpc
//it is a singleton class and will also serve for network creation
public class Server {
	//class variable to hold the singleton
	private static Server serverInstance = null;
	
	//instance variable IP
	private final String IP;
	
	//instance variable port
	private final int port;
	
	//webServer instance variable
	private final WebServer webServer;
	
	//ArrayList named network to hold ip address of hosts in the net
	private final ArrayList<String> network = new ArrayList<String>();
	
	//ArrayList to hold the nodes the network has. Nodes are actually clients
	private final ArrayList<Client> clients = new ArrayList<Client>();
	
	//priority variable to be assigned to each client when joining
	//it will be directly provided by singleton server instance to all joined clients
	//it starts from 1, because 0 is captured by server. And each time new client
	//joins the network it will incremented and returned.
	private int nextPriority = 1;
	
	//data structure to hold the ips which is in critical section//, get concurrent set
	private final ConcurrentLinkedQueue<String> criticalSection = 
			new ConcurrentLinkedQueue<String>();
	//private final Set<String> criticalSection 
			//= Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
			//new HashSet<String>();
	
	//data structure to hold the ips which is in the waiting queue for the desired resource
	//will be used by ----- CENTRALIZED MUTUAL EXCLUSION ALGORITHM -------
	private final LinkedBlockingDeque<String> waitingQueue = 
			new LinkedBlockingDeque<String>();

	//factory method, returns Server with port 8080
	public static Server getInstance() {
		if (serverInstance == null) {
			serverInstance = new Server();
		}
		return serverInstance;
	} // getInstance
	
	//this factory method returns Server singleton with specified port
	public static Server getInstance(String socketAddress) {
		if (serverInstance == null) {
			serverInstance = new Server(socketAddress);
		}
		return serverInstance;
	} // getInstance
	
	//default constructor
	private Server()
	{
		//in this case the default port 8080 will be assigned and constructor will be called
		//also ip address will be localhost
		this(Utils.toSocketAddress("localhost", 8080));
		
	} // Server
	
	//constructor method
	private Server(String socketAddress)
	{
		this.IP = Utils.getIPFromSocketAddress(socketAddress);
		this.port = Utils.getPortFromSocketAddress(socketAddress);
		
		//now get the xmlrcpserver of this minimal http webserver
		//and set the configuration for it.
		this.webServer = new WebServer(port);
		
		//WebServer(int pPort, java.net.InetAddress pAddr)
        //Creates a web server at the specified port number and IP address.
		
		//xmlrpcserver object receives and executes xml rpc calls from clients
		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
		
		//now define the handling mechanism for rpc requests by clients
		//this is achieved by PropertyHandlerMapping
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		
		//enables Handler class to have void methods, in this case
		//RequestProcessor class can have void methods now.
		//phm.setVoidMethodEnabled(true);
		
		//addHandler method accepts the string name of class we want to execute the method at
		//and it class instance variable
		try {
			phm.addHandler("RequestProcessor", RequestProcessor.class);
		} catch (XmlRpcException e) {
			System.err.println("Error while adding handler RequestProcessor");
		} // catch
		
		//set the handler
		xmlRpcServer.setHandlerMapping(phm);
		
		 /* Load handler definitions from a property file.
         * The property file might look like:
         *   RequestProcessor=org.apache.xmlrpc.demo.Calculator
         *   org.apache.xmlrpc.demo.proxy.Adder=org.apache.xmlrpc.demo.proxy.AdderImpl
         */
        //phm.load(Thread.currentThread().getContextClassLoader(),
                 //"MyHandlers.properties");
		
		//xmlrpcserver has to have configuration set as was in the client
		XmlRpcServerConfigImpl xmlRpcServerConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		//enable extensions for transfering of any values including 
		//long, short, byte, float, DOM nodes, java.io.Serializable, JAXB objects etc.
		//which is an extension to the XML-RPC specification which says only Objects
		//can be transfered
		xmlRpcServerConfig.setEnabledForExtensions(true);
		
		//also enable for exceptions which make it possible for clients catch
		//the exception thrown in server's side
		//xmlRpcServerConfig.setEnabledForExceptions(true);
		
		//the following code enables the faster streaming mode, we do not actually need it.
		//xmlRpcServerConfig.setContentLengthOptional(true);
		
		//add the server to the network
		network.add(IP);
		
	} // Server
	
	//check whether the server is in the network
	public boolean isNetworkAvailable()
	{
		//server ip address resides in the first index
		return network.get(0) != null;
	} // isNetworkAvailable
	
	//check whether or not there is at least one host
	public boolean atLeastOneHostAvailable()
	{
		//it will happen when the network size is bigger than or equal to 2
		//in case of 2, we will have one server and one client
		return network.size() >= 2;
	} // atLeastOneHostAvailable
	
	//check whether new joining host was in the network, if it is we will not join it.
	public boolean checkHostEligibility(String IP)
	{
		return network.contains(IP);
	} // isInTheNetwork
	
	//method to add a host to the network
	public void addHost(String socketAddress)
	{
		String IP = Utils.getIPFromSocketAddress(socketAddress);
		//update the network
		network.add(IP);
	} // addHost
	
	//method to remove specified host from the network
	public void removeHost(String nodeIP)
	{
		//update the network
		network.remove(nodeIP);
	} // removeHost
	
    //converts the arraylist to array and returs
	public String[] getNodeArray() {
		return network.toArray(new String[]{});
 	} // getNodeArray
	
	public int getPort() {
		return port;
	} // getPort

	public WebServer getWebServer() {
		return webServer;
	} // getWebServer
	
	public String getIP() {
		return IP;
	} // getIP

	//start the server
	public void start()
	{
		try {
			//start the server and print out Success message
			webServer.start();
			System.out.println("Server started successfully!");
			
		} catch (IOException e) {
			/*System.err.println("IOException at the server startup.");*/
			System.out.println("Server has already been started, proceed with operations.");
		} // catch
	} // start
	
	//shut down the server
	public void stop()
	{
		webServer.shutdown();
		System.out.println("Server stopped successfully!");
	} // shutdown

	//get the socket address of the server
	public String getSocketAddress()
	{
		return Utils.toSocketAddress(IP, port);
	} // getSocketAddress

	//return the arraylist of ip address of the network
	public ArrayList<String> getNetwork() {
		return network;
	} // getNetwork
	
	//method to update the clients list
	//this will be called by the joinNode method in RequestProcessor class
	//Before call to joinNode the problem joining the client with same ip
	//address handled
	public void updateClientList(String clientSocketAddress)
	{
		try {
			Client client = new Client(getSocketAddress(), clientSocketAddress);
			//set the priority for client, we will server.getNextPriority to get
			//a unique priority for the client
			client.setPriority(Server.getInstance().getNextPriority());
			clients.add(client);
		} catch (MalformedURLException e) {
			System.err.println("Exception in updating client's list on the server"
					+ "\nClient's list will not be changed");
		} // catch
	} // updateClientList
	
	//the method to remove the client by ip from the clients list
	public void removeClientByIP(String IP)
	{
		//we will loop through all the clients
		//find the client with given ip and remove from the client's list
		//and once again there is no clients with the same IP address in the network
		for (Client client : clients)
		{
			//if we find the client with the given ip we remove if from the list and 
			//break the loop which will cause method termination
			if (client.getIP().equals(IP))
			{
				clients.remove(client);
				break;
			} // if
		} // for
		
	} // removeClientByIP
	
	//method to get the client by IP
	public /*synchronized*/ Client getClientByIP(String IP)
	{
		Client clientToBeReturned = null;
		
		//we will loop through all the clients
		//find the corresponding client with specified IP
		//and return it back
		for (Client client : clients)
		{
			if (client.getIP().equals(IP))
			{
				clientToBeReturned = client;
			} // if
		} // for
		
		//in case the client not found, null will be returned
		return clientToBeReturned;
	} // getClientByIP
	
	//method to broadcast messages
	//we do include the server in the broadcast array
	public String[] getBroadcastNodes() {
		//we will not broadcast to last added node
		String[] broadCastNodes = new String[getNodeArray().length - 1];
		
		for (int index = 0; index < broadCastNodes.length; index ++)
		{
			broadCastNodes[index] = getNodeArray()[index];
		} // for
		
		return broadCastNodes;
	} // getBroadcastNodes
	
	//do the same broadcasting thing for clients
	//get the clients to broadcast messages
	public Client[] getBroadcastClients()
	{
		//we will not broadcast to last added client
		Client[] broadCastClients = new Client[clients.size() - 1];
		for (int index = 0; index < broadCastClients.length; index ++)
		{
			broadCastClients[index] = clients.get(index);
		} // for
		
		return broadCastClients;
	} // getBroadcastClients

	//get the clients currently in the network
	public ArrayList<Client> getClients() {
		return clients;
	} // getClients
	
	//gets the first remaining client, this method is called during signoff operation
	public Client getFirstRemainingClient()
	{
		return clients.get(0);
	} // getFirstRemainingClient
	
	//helper method to enumerate the priorities for clients
	public void enumerateClientsPriorities()
	{
		//priorities are something like one based indices 
		int startingPriority = 1;
		for (Client client : clients)
		{
			client.setPriority(startingPriority);
			startingPriority ++;
		} // for
	} // enumerateClientsPriorities
	
	//method to reset nodes for coordinator election
	//this will be called during join and signOff operations
	//to reset each clients isMasterNode string to false
	//and in coordinator election process one of these clients
	//will be set as master node again
	public void resetNodesForCoordinatorElection()
	{
		//enumerate clients to reset their isMasterNode string to false
		for (Client client : clients)
		{
			client.setMasterNode(false);
		} // for
	} // resetNodesForCoordinatorElection
	
	//method to get the master node
	public Client getMasterNode()
	{
		//get the master by looping through all the clients in the network
		for (Client client : clients)
		{
			if (client.isMasterNode())
				return client;
		} // for
		
		//if this return is reached it means, there is no master node, we return null
		return null;
	} // getMasterNode
	
	// getter method that will be used by client instance when it initializes
	// itself
	public int getNextPriority() {
		return nextPriority++;
	} // getNextPriority

	// when every client signs off from the network, this should initialiazed to
	// 1 again
	public void resetNextPriority() {
		nextPriority = 1;
	} // resetNextPriority
	
	//helper method to return array of clients instead of arraylist
	public Client[] getClientsArray()
	{
		return getClients().toArray(new Client[] {});
	} // getClientsArray
	
	//get a shallow copy of clients, where original clients arraylist instance
	//want to be affected. But the clients will be the same as in the original arraylist
	public ArrayList<Client> getShallowCopyClients()
	{
		//get the clients, make a shallow copy
		//original client arraylist does not change
		ArrayList<Client> clientList = new ArrayList<Client>();
		
		//for each client add the client to the copy arraylist
		for(Client client : clients)
		{
			clientList.add(client);
		} // for
		
		return clientList;
	} // getShallowCopyClients
	
	//convenience method to get the array representation of shallow copy arraylist
	public Client[] getShallowCopyClientsArray()
	{
		return getShallowCopyClients().toArray(new Client[] {});
	} // getShallowCopyClientsArray
	
	//gets the clients other than this client
	//Client's equals method will be used heavily here
	public ArrayList<Client> getClientsOtherThan(Client thisClient)
	{
		// get the clients, create an array list and different clients
		// other than this client to arraylist
		ArrayList<Client> clientList = new ArrayList<Client>();

		// for each client add the client to the copy arraylist
		for (Client client : clients) {
			if ( !client.equals(thisClient) )
			{
				clientList.add(client);
			} // if
		} // for

		return clientList;
	} // getClientsOtherThan
	
	//convenience array representation for getClientsOtherThan method
	public Client[] getClientsArrayOtherThan(Client thisClient)
	{
		return getClientsOtherThan(thisClient).toArray(new Client[] {});
	} // getClientsArrayOtherThan
	
	//method to generate a random word
	public String generateRandomEnglishWord() {
		
		//get the corpus of words
		String[] corpus = getCorpus();
		
		//random to generate random index
		Random random = new Random();
		
		//create a list from it
		ArrayList<String> wordList = new ArrayList<String>(Arrays.asList(corpus));
		
		//shuffle them out
		Collections.shuffle(wordList);
		
		//get the random index
		int randomIndex =  random.nextInt(wordList.size());
		
		return wordList.get(randomIndex);
		
	} // generateRandomEnglishWord
	
	//helper method to create a corpus of words
	private String[] getCorpus()
	{
		return new String[]
				{
				"when", "in", "the", "course", "of", "human", "events", "it", "becomes", 
				"necessary", "book", "shelf", "paper", "phone",
				"as", "gregor", "samsa", "awoke", "one", "morning", "from", "uneasy", 
				"dreams", "he", "they", "you", "imagine", "web", "window",
				"found", "himself", "transformed",
				"his", "bed", "into", "a", "gigantic", "insect",
				"far", "out", "uncharted", "backwaters", "unfashionable", 
				"end", "of", "western", "spiral", "arm", "of", "galaxy", "lies", 
				"small", "regarded", "yellow", "sun",
				 "truth", "universally", "acknowledged",
				 "not", "nasty", "dirty", "wet", "hole", "filled", "with", "ends", "of", "worms", 
				 "an", "oozy", "smell", "nor", "yet", "dry", "bare", "sandy", "hole", 
				 "with", "nothing", "in", "it", "to", "sitdown", "on", "or", "to", 
				 "eat", "was", "hobbit", "hole", "that", "means", "comfort"
				} ;
	} // getCorpus
	
	//method to retrieve the waiting queue
	public LinkedBlockingDeque<String> getWaitingQueue() {
		return waitingQueue;
	} // getWaitingQueue
	
	//method to get a head of the queue
	public String getWaitingQueueHead()
	{
		return waitingQueue.peek();
	} // getHead
	
	// helper method to assign cliens with specific ip to critical section
	public void assignToCriticalSection(String requesterIP) {
		criticalSection.add(requesterIP);
	} // assignToCriticalSection

	public void removeFromCriticalSection(String requesterIP) {
		 criticalSection.remove(requesterIP);
		 //criticalSection.clear();
	} // removeFromCriticalSection

	// helper method to return whether or not the critical section is empty
	public boolean isCriticalSectionEmpty() {
		return criticalSection.isEmpty();
	} // isCriticalSectionEmpty

	// helper method to queue request which has been denied for a limited amount
	// of time
	public void queueRequest(String requesterIP) {
		waitingQueue.add(requesterIP);
	} // queueRequest

	// helper method to detect whether the waiting queue is empty
	public synchronized boolean isQueueEmpty() {
		return waitingQueue.isEmpty();
	} // isQueueEmpty
	
	/**
	 * Helper method to dequeue (return and remove) first 
	 * element from the waiting queue
	 */
	public String dequeue() {
		return waitingQueue.poll();
	} // dequeue
	
	//helper method to clear the queue
	public void emptyQueue()
	{
		waitingQueue.clear();
	} // emptyQueue

	//getCriticalSection
	public ConcurrentLinkedQueue<String> getCriticalSection() {
		return criticalSection;
	} // getCriticalSection
	
} // class Server
