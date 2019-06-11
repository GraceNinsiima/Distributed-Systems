
import java.net.*;
//import java.net.UnknownHostException;
import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;

public class Group implements Runnable
{
         protected InetAddress grpIpAddress;
         int maxbuf=1024;
         protected int port = 4446 ;
         protected boolean status;
         MulticastSocket multicastsock;
         DatagramPacket packet;
         InetAddress lclhost;
         String myAddr;
         Sequencer sequencer;
         SequencerImp sequencerimp;
         long lastSequenceRecd;
         long lastSequenceSent;
         long lastSendTime;
         String myName;
         Thread heartBeater;
         MsgHandler handler;
         
         DatagramSocket socket;
    public Group(String host, MsgHandler handler, String senderName)  throws GroupException, SequencerException, NotBoundException, IOException
    {
    	lastSequenceRecd=-1L;
        lastSequenceSent=-1L;
    	
    	//get any object with caption MySequencer from the RMI registry...and its type has to be of the interface sequencer
    	sequencer = (Sequencer)Naming.lookup("/MySequencer");
    	
    	//gets the address of the local host
    	lclhost = InetAddress.getLocalHost();
    	
    	//combines the localhost address and the senders name 
    	myAddr =senderName+lclhost;
    	
    	SequencerJoinInfo info = sequencer.join(myAddr);
    	//retrive goup ip address form join in sequencerimpl
    	
    	grpIpAddress = info.addr;
    	//retrive last received sequence number form join in sequencerimpl
    	lastSequenceRecd = info.sequence;
    	
    	//create multisocket where to listen
    	multicastsock = new MulticastSocket(4446); 
    	
    	//jooin group??
    	multicastsock.joinGroup(grpIpAddress);
    	this.handler=handler;
    	 Thread myThread = new Thread(this);//create a thread
		 myThread.start();//connects to the run method
		 heartBeater = new HeartBeater(5);
		 heartBeater.start();
    	
    	
    }

    public void send(byte[] msg) throws Exception
    {
    	
    	if(multicastsock!=null)//check if there is a global/multicasting socket specified
    	{
    		try{
    			//send the message to the sequencer so that it is mashalled.
                         lastSequenceSent = ++lastSequenceSent;
                         System.out.println("Message contains "+myAddr+","+msg+","+lastSequenceSent+","+lastSequenceRecd);
    			sequencer.send(myAddr,msg,lastSequenceSent,lastSequenceRecd);
    			//change the last send time to long
    			lastSendTime=(new Date()).getTime();
    			
    		}catch(Exception e){
    			System.out.println("couldn't contact sequencer because of this issue "+ e);
    			throw new GroupException("couldn't send to sequencer");
    			
    		}
    	}else{
			throw new GroupException("Group not joined");
		}
    	
    }

    public void leave()
    {
       // leave group
    	if(multicastsock!=null)//check if there is a global/multicasting socket specified
    	{
    		try{
                socket = new DatagramSocket();
    		multicastsock.leaveGroup(grpIpAddress);
    		sequencer.leave(myAddr);
    		}catch(Exception e){System.out.println("couldn't leave group"+ e);}
    	}
    }

    public void run()
    {
        // repeatedly: listen to MulticastSocket created in constructor, and on receipt
        // of a datagram call "handle" on the instance
        // of Group.MsgHandler which was supplied to the constructor
    	try{
            while(true){
                //System.out.println("Hello Karanzi");
          byte buffer[]= new byte[maxbuf];
    	
    	//create the datagram packet to recieve the multicat message
    	packet = new DatagramPacket(buffer,buffer.length);
        multicastsock.receive(packet);
    	//unmashal
    	
    	ByteArrayInputStream bstream = new ByteArrayInputStream(buffer,0,packet.getLength());
    	DataInputStream dstream = new DataInputStream(bstream);
    	long gotSequence = dstream.readLong();
    	int count =dstream.read(buffer);
    	long wantSeq = lastSequenceRecd + 1L;
    	 if(lastSequenceRecd>=0 && wantSeq<gotSequence){
    		 for(long getSeq=wantSeq;getSeq<gotSequence;getSeq++){
    			 byte[] extra = sequencer.getMissing(myAddr,getSeq);
    			 int countExtra = extra.length;
    			 System.out.println("get missing sequence number"+getSeq);
    			handler.handle(countExtra,extra);
    		 }
    	 }
    	       lastSequenceRecd = gotSequence;
    	       handler.handle(count,buffer);
            }
    	}catch(Exception e){
    		System.out.println("error"+e);
    		e.printStackTrace();
    	}

    	   }

    public interface MsgHandler
    {
         public void handle(int count, byte[] msg);
    }

    public class GroupException extends Exception
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public GroupException(String s)
        {
            super(s);
        }
    }

    public class HeartBeater extends Thread
    {
        // This thread sends heartbeat messages when required
    	public void run(){
    		do
    			 try{
    				 do
    					 Thread.sleep(period*1000);
    				 while((new Date()).getTime() - lastSendTime<(long)(period*1000));
    				 sequencer.heartbeat(myName, lastSequenceRecd);
    					 
    			 }catch(Exception error){}
    		while(true);
    	}
    	int period;
    	public HeartBeater(int period){
    		this.period=period;
    	}
    }
  
    }

