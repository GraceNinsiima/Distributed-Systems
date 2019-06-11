
import java.io.*;
import java.net.*;

public class SequencerJoinInfo implements Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public InetAddress addr;
    public long sequence;

    public SequencerJoinInfo(InetAddress addr, long sequence)
    {
        this.addr = addr;
        this.sequence = sequence;
    }
}
