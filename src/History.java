
import java.io.*;
import java.util.*;
public class History extends Hashtable{
    //Create a hashTable to store the contents of the message, sender, sequence number.
    private Hashtable history;
    public static final int MAX_HISTORY = 1024;
    public long historyCleanedTo;
    //Create constructor for history
    public History(){
        history = new Hashtable();
        historyCleanedTo = -1L;
    }
    //And the sender to the history
    public void noteReceived(String sender, long received){
        history.put(sender, new Long(received));
    }
    //Add the message to the history buffer
    public void addMessage(String sender, long sequenceNo, byte [] msg){
        //check if the message is not null.
        if (msg!=null){
            put(new Long(sequenceNo), msg);
        }
        //removing already received values in the buffer
        if(size()>1024){
            long min = 0x7fffffL;
            //Getting the different senders in the hash table
            for(Enumeration enum1 = history.keys(); enum1.hasMoreElements();){
                String sent = (String)enum1.nextElement();
                Long got = (Long)history.get(sent);
                long have = got.longValue();// the already received datagram
                if(have<min)
                    min = have;//sent the new mininum number;
            }
            //removing datagrams that are not needed in the buffer
            for(long s = historyCleanedTo+1L; s<=min; s++){
                remove(new Long(s));
                historyCleanedTo = s;
            }
        }
    }
    //Getting the missing datagram
     public byte[] getMsg(long sequenceNumber){
            return (byte[])get(new Long(sequenceNumber));
        }
     //Remove sender
     public synchronized void eraseSender(String sender){
         history.remove(sender);
     }
}
