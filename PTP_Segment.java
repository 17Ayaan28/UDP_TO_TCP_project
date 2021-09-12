import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * PTP_Segment represents a segment of the reliable transport protocol
 */
public class PTP_Segment {
    
    private byte Synbit;
    private byte Finbit;
    private byte[] seq_number;
    private byte[] ack_number;
    private byte[] file_data;

    PTP_Segment (Boolean Synbit, Boolean Finbit, Integer seq_number, Integer ack_number, byte[] file_data ) {

        if (Synbit == true) {
            this.Synbit = 1;
        } else {
            this.Synbit = 0;
        }

        if (Finbit == true) {
            this.Finbit = 1;
        } else {
            this.Finbit = 0;
        }
        
        this.seq_number = convertIntToBytes(seq_number);
        this.ack_number = convertIntToBytes(ack_number);

        this.file_data = file_data;
    }

    public byte[] convertIntToBytes(Integer n){

        ByteBuffer byte_array = ByteBuffer.allocate(4); 
        byte_array.putInt(n); 
        return byte_array.array();
    }

    public byte[] convertPTPTOUDP(){

        ArrayList<Byte> UDP_Segment = new ArrayList<Byte>();

        UDP_Segment.add(this.Synbit);
        UDP_Segment.add(this.Finbit);

        for (byte byte1 : this.seq_number) {
            UDP_Segment.add(byte1);
        }

        for (byte byte1 : this.ack_number) {
            UDP_Segment.add(byte1);
        }

        for (byte byte1 : this.file_data) {
            UDP_Segment.add(byte1);
        }

        byte[] result = new byte[UDP_Segment.size()];
        for(int i = 0; i < UDP_Segment.size(); i++) {
            result[i] = UDP_Segment.get(i).byteValue();
        }
    
        return result;
    }

    public byte getSynbit() {
        return Synbit;
    }

    public void setSynbit(byte synbit) {
        Synbit = synbit;
    }

    public byte getFinbit() {
        return Finbit;
    }

    public void setFinbit(byte finbit) {
        Finbit = finbit;
    }

    public Integer getSeq_number() {
        return byteArrayToInt(this.seq_number);
    }

    public void setSeq_number(byte[] seq_number) {
        this.seq_number = seq_number;
    }

    public Integer getAck_number() {
        return byteArrayToInt(this.ack_number);
    }

    public void setAck_number(byte[] ack_number) {
        this.ack_number = ack_number;
    }

    public byte[] getFile_data() {
        return file_data;
    }

    public void setFile_data(byte[] file_data) {
        this.file_data = file_data;
    }

    public static int byteArrayToInt(byte[] b) {
        
        int a = ByteBuffer.wrap(b).getInt();
        return a;
    }
}
