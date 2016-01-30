# Principles Of Distributed Systems

This repo contains command line tool with the following functionalities:

**1. Connectivity**

*a)* It interconnects several hosts to a logical network. It makes it possible that all nodes connected to all other nodes.  

*b)* **Join** operation allows a new node to join the network of already connected nodes.

*c)* New machines join the network by sending a join message to one of the machines already in the network. 
address of the new host is thereupon propagated in the network.

*d)* Hosts is also able to **sign off** from the network again.

*e)* One node in the network is elected as master node. The master node stores a string variable that is initially empty.

**2. Distributed Read and Write Operations**

a) Once the network is established any node can start a distributed read / write process by 
sending a **start** message to all the other nodes in the network. 
b) The  process  takes  20  seconds.  During  this  time  all  the  nodes 
in  the  network  do  the following: 
LOOP
...1.   Wait a random amount of time 
...2.  Read the string variable from the master node 
...3. Append some random english word to this string 
...4.  Write the updated string to the master node 
END LOOP
c) After the process has ended all the nodes read the final string from the master node and write  it  to  the  screen.
Moreover  they  check  if  all  the  words  they  added  to  the  string  are present in the final string.
The result of this check is also written to the screen.  
d) To make this work, the read and write operations are synchronised using two  different  mutual 
exclusion algorithms: **Centralised Mutual Exclusion and Ricart & Agrawala.**
e) The master is elected by the **Bully algorithm.**

C# and Java implementations  are  interoperable.  For  this  purpose,  XML-RPC cross platform technology 
for the inter-machine communication is used in both implementations.
