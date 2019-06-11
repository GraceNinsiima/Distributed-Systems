
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.net.UnknownHostException;

public class SequencerImp extends UnicastRemoteObject implements Sequencer{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	History history;
	Vector myClients;
	InetAddress grpIP;
	MulticastSocket multicastsock;
	final int port = 4446;
	final int MAX_MSG_LENGTH = 1024;
	String name;
	int sequenceNo;
	
	//main
    public static void main(String[] a){
    	
    	
    	try{
    		SequencerImp impl= new SequencerImp("MySequencer");
    		//linking object imp with RMI registry
    		Naming.rebind("/MySequencer",impl);
    		System.out.println("Ready to continue...");//binding is ok
    	}
    	catch(Exception err)
    	{System.out.println("The error is due to..."+err);
    	err.printStackTrace();
    	}
    	
    }
    
	public SequencerImp() throws Exception{
		super();
		
	} 
	public SequencerImp(String name) throws RemoteException{
		//initialisations of some
		this.name= name;
		try{
			
			history = new History();
			myClients= new Vector();
			multicastsock = new MulticastSocket(port);
			grpIP = InetAddress.getByName("224.6.7.8");
			}catch(Exception exc)
			{
				System.out.println("couldnt initiallise sequencerimpl"+exc);
				exc.printStackTrace();
			}
		
	}
	public SequencerJoinInfo join(String sender)
		        throws RemoteException, SequencerException, UnknownHostException{
		       /* InetAddress address = InetAddress.getByName(sender);
		        long sequenceNo = 0;*/
		if(myClients.contains(sender))
		{
			throw new SequencerException(sender+"not unique");
		}else{
			myClients.addElement(sender);
			history.noteReceived(sender,sequenceNo);
			return new SequencerJoinInfo(grpIP, sequenceNo);
			
		}
		        
		    }
		    //send method
		    public void send(String sender, byte[] msg, long msgID, long lastSequenceReceived)
		        throws RemoteException{
		    	try{
		    	//Marsghalling the data
		        ByteArrayOutputStream bstream = new ByteArrayOutputStream(MAX_MSG_LENGTH);
		        DataOutputStream dstream = new DataOutputStream(bstream);
		        dstream.writeLong(++sequenceNo);
		        dstream.write(msg,0,msg.length);
		        DatagramPacket message = new DatagramPacket(bstream.toByteArray(),bstream.size(),grpIP,port);
		        multicastsock.send(message);
		    	}catch(Exception ex){System.out.println("couldnt send message"+ex);}
		    	//graace need
		    	history.noteReceived(sender, lastSequenceReceived);
		    	history.addMessage(sender, sequenceNo, msg);
		        
		        
		    	
		    }
		    
		 // leave -- tell sequencer that "sender" will no longer need its services
		    public void leave(String sender)
		         throws RemoteException{
		    	//remove the client name from the sequencer list
		    	myClients.removeElement(sender);
		    	
		    	//remove from oour history file
		    	history.eraseSender(sender);
		    }
		    
		 // getMissing -- ask sequencer for the message whose sequence number is "sequence"
		    public byte[] getMissing(String sender, long sequence)
		         throws RemoteException, SequencerException{
		    	byte exist[] = history.getMsg(sequence);
		    	if(exist!=null)//if the number is there
		    	{
		    		System.out.print("SSequencer gets missing"+sequence);
		    		return exist;
		    	}else{
		    		System.out.print("Sequencer couldn't get sequence number"+sequence);
		    		throw new SequencerException("couldn't get sequence number"+sequence);
		    	}
		    }
		    
		 // heartbeat -- we have received messages up to number "lastSequenceReceived"
		    public void heartbeat(String sender, long lastSequenceReceived)
		         throws RemoteException{
		    	System.out.print(sender+"Heartbeat:"+lastSequenceReceived);
		    	history.noteReceived(sender,lastSequenceReceived);
		    }
            /// recieve
                    public void receive() throws IOException{
                        byte [] buffer = new byte[1000];
                        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                        multicastsock.receive(messageIn);
                        String receivedMessage = new String(messageIn.getData());
                        System.out.println(receivedMessage);
                    }
		    
		    
		    
}
