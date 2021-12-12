import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;


public class Sender {

    public static int current_window_size;
    public static int last_sq_num_of_server; // next acknum = this + 1
    public static int sq_num_client;
    public static int ack_num_after_hs;
    public static int index_of_file_array = 0;

    public static InetAddress address;
    public static int port;
    public static int filename;
    public static int mws;
    public static int mss;
    public static int timeout;
    public static DatagramSocket d_socket;
    public static ArrayList<byte[]> f_d; 
    public static Thread t_n;

    /*
    public Thread t;
    public static int current_window_size;
    public static int sr_sq_cl_ak;
    public static int sr_ak_cl_sq;
    public int i = 0; // file_data_sent
    public ArrayList<byte[]> f_data;

    public PTP_Sender() {
        super();
    }
    */

    public static void main(String[] args) throws IOException {

        InetAddress receiver_host = InetAddress.getByName(args[0]);

        Integer receiver_port = Integer.parseInt(args[1]);
        String file_name = args[2];

        Integer MWS = Integer.parseInt(args[3]);
        Integer MSS = Integer.parseInt(args[4]);
        String timeout = args[5];

        System.out.println("Receiver Host:   " + receiver_host);
        System.out.println("Receiver Port:    " + receiver_port);
        System.out.println("File name:  " + file_name);
        System.out.println("MWS:  " + MWS);
        System.out.println("MSS:  " + MSS);
        System.out.println("timeout:  " + timeout);

        DatagramSocket client_socket = new DatagramSocket();
        d_socket = client_socket;
        address = receiver_host;
        port = receiver_port;

        handshake(client_socket, receiver_host, receiver_port);

        System.out.println("-------- checking vars after handshake -----------");
        System.out.println("Ack number after hs:   " + last_sq_num_of_server);
        System.out.println("sq number after hs: " + sq_num_client);
        System.out.println("--------------------------------------------------");

        send(client_socket ,receiver_host, receiver_port, file_name, MWS, MSS, timeout);

        terminate_connection();
        //client_socket.close();
        return;

    }
     
    // Added throws declaration for now
    // Later surround with try_catch
    
    public static ArrayList<byte[]> convertFile (String filename, Integer MSS) throws IOException {

        File file = new File(filename);

        ArrayList<byte[]> file_bytes = new ArrayList<byte[]>();

        byte[] b_array = new byte[MSS];

        
        try {
            FileInputStream fileInputStream = new FileInputStream(file);

            while(true) {

                if (fileInputStream.available() == 0){
                    break;
                }
    
                if (fileInputStream.available() < MSS) {
                    b_array = new byte[fileInputStream.available()];
                }
    
                fileInputStream.read(b_array);
                file_bytes.add(b_array);
    
            }
            
            fileInputStream.close();
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return file_bytes;

    }
    
    public static PTP_Segment convertUDPTOPTP(DatagramPacket p) {

        byte[] res = p.getData();
        byte s_b = res[0];
        byte f_b = res[1];

        ArrayList<Byte> s_n = new ArrayList<Byte>();

        ArrayList<Byte> a_n = new ArrayList<Byte>();

        ArrayList<Byte> f_d = new ArrayList<Byte>();

        for(int i = 2; i <= 5; i++){
            s_n.add(res[i]);
        }

        for(int i = 6; i <= 9; i++){
            a_n.add(res[i]);
        }

        if (p.getLength() > 10){

            for(int i = 10; i < res.length; i++){
                f_d.add(res[i]);
            }

            byte[] s_n_bt = new byte[s_n.size()];

            for(int i = 0; i < s_n.size(); i++) {
                s_n_bt[i] = s_n.get(i).byteValue();
            }

            byte[] a_n_bt = new byte[a_n.size()];

            for(int i = 0; i < a_n.size(); i++) {
                a_n_bt[i] = a_n.get(i).byteValue();
            }

            byte[] f_d_bt = new byte[f_d.size()];

            for(int i = 0; i < f_d.size(); i++) {
                f_d_bt[i] = f_d.get(i).byteValue();
            }

            PTP_Segment packet = new PTP_Segment(s_b, f_b, s_n_bt, a_n_bt, f_d_bt);

            return packet;

        } else {

            byte[] s_n_bt = new byte[s_n.size()];

            for(int i = 0; i < s_n.size(); i++) {
                s_n_bt[i] = s_n.get(i).byteValue();
            }

            byte[] a_n_bt = new byte[a_n.size()];

            for(int i = 0; i < a_n.size(); i++) {
                a_n_bt[i] = a_n.get(i).byteValue();
            }

            PTP_Segment packet = new PTP_Segment(s_b, f_b, s_n_bt, a_n_bt, new byte[0]);

            return packet;
        }

    }
    
    public static void setCurrentWindowSize(Integer n) {
        current_window_size = n;
        send_file(d_socket, f_d, address, port);
        // call sender here
    }

    public static int getCurrentWindowSize() {
        return current_window_size;
    }
    
    public static void send(DatagramSocket socket ,InetAddress address, Integer port, String filename, Integer MWS, Integer MSS, String timeout) {
          
        current_window_size = MWS/MSS;

        System.out.println("--------- current window size" + current_window_size + "-------------");

        Runnable r2 = new Runnable(){ 

            public void run(){  

                System.out.println("Client is listening for acks :    ");

                while(true) {
        
                    byte[] receiveData=new byte[1024];
                    DatagramPacket arrivedpacket = new DatagramPacket(receiveData, receiveData.length);
        
                    try {
                        socket.receive(arrivedpacket);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
        
                    PTP_Segment ptp_seg = convertUDPTOPTP(arrivedpacket);
                    
                    System.out.println("------- Acknowledgements -------");
                    System.out.println("Sequence number:   " + ptp_seg.getSeq_number());
                    System.out.println("Ack number:     " + ptp_seg.getAck_number());
                    System.out.println("syn bit:    " + ptp_seg.getSynbit());
                    System.out.println("--------------------------------");
        
                    last_sq_num_of_server = ptp_seg.getSeq_number();
        
                    setCurrentWindowSize(getCurrentWindowSize() + 1);

                    if (index_of_file_array >= f_d.size()) {
                        return;
                    }
        
                }
            }

        };  

        //Thread t1=new Thread(r1);  
        Thread t2=new Thread(r2);  
        t_n = t2;
        //t1.start();  
        t2.start();
        
        
        ArrayList<byte[]> file_data;
        try {
            file_data = convertFile(filename, MSS);
            f_d = file_data;

            send_file(socket, file_data, address, port);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public static void send_file(DatagramSocket socket, ArrayList<byte[]> file_data, InetAddress address, Integer port) {
        
        if (index_of_file_array < file_data.size()) {

            int val_to_be_reduced_from_window = 0;

            for(int n = 0; n <= current_window_size; n++) {

                if (index_of_file_array >= (file_data.size())) {
                    break;
                }

                PTP_Segment file_segment = new PTP_Segment(false, false, last_sq_num_of_server, sq_num_client, file_data.get(index_of_file_array));
                last_sq_num_of_server = last_sq_num_of_server + file_data.get(index_of_file_array).length;
                byte[] bin_file_segment = file_segment.convertPTPTOUDP();
                DatagramPacket udp_file_segment = new DatagramPacket(bin_file_segment, bin_file_segment.length, address, port);

                System.out.println("---- Packet to be sent ------");
                System.out.println("seq :   " + file_segment.getSeq_number());
                System.out.println("ack :   " + file_segment.getAck_number());
                System.out.println("data :   " + file_segment.getFile_data().length);
                System.out.println("---- ------------ ------");

        
                try {
                    socket.send(udp_file_segment);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                val_to_be_reduced_from_window++;
                index_of_file_array++;

            }

            current_window_size = current_window_size - val_to_be_reduced_from_window;

        } 
        //else {
            //t_n.interrupt();
        //}

        //if (index_of_file_array >= (file_data.size())) {
        //    t_n.interrupt();
        //}
    }

    
    public static void handshake(DatagramSocket socket, InetAddress address, Integer port) throws IOException {

        System.out.println("---------Handshake----------");

        PTP_Segment p = new PTP_Segment(true, false, 1, 0, new byte[0]);

        System.out.println("Sequence number:   " + p.getSeq_number());
        System.out.println("Ack number:     " + p.getAck_number());
        System.out.println("syn bit:    " + p.getSynbit());

        
        byte[] b = p.convertPTPTOUDP();

        DatagramPacket d = new DatagramPacket(b, b.length, address, port);

        socket.send(d); // 1st segment sent  
        
        byte[] receiveData=new byte[b.length];
        
        // receive from server
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        socket.receive(receivePacket);

        PTP_Segment syn_ack = convertUDPTOPTP(receivePacket);

        System.out.println("-------- Incoming PDP Syn-ACK --------");
        System.out.println("syn bit: " + syn_ack.getSynbit());
        System.out.println("Seq_number: " + syn_ack.getSeq_number());
        System.out.println("Ack_number: " + syn_ack.getAck_number());
        System.out.println("-----------------");

        
        PTP_Segment final_ack_of_handshake = new PTP_Segment(false, false ,  2, syn_ack.getSeq_number() + 1, new byte[0]);
        byte[] bin_final_ack = final_ack_of_handshake.convertPTPTOUDP();

        DatagramPacket d2 = new DatagramPacket(bin_final_ack, bin_final_ack.length, address, port);
        socket.send(d2);

        byte[] rd_2=new byte[b.length];
        
        // receive from server
        DatagramPacket r = new DatagramPacket(rd_2,rd_2.length);
        socket.receive(r);

        PTP_Segment ack = convertUDPTOPTP(r);


        System.out.println("-------- Incoming PDP ACK --------");
        System.out.println("syn bit: " + ack.getSynbit());
        System.out.println("Seq_number: " + ack.getSeq_number());
        System.out.println("Ack_number: " + ack.getAck_number());
        System.out.println("-----------------");

        System.out.println("Handshake complete");

        sq_num_client = 3;
        //ack_num_after_hs = 3;
        last_sq_num_of_server = 3;
        
    }

    public static void terminate_connection () {

        System.out.println("------ Terminate Connection -------");

        // Client fin segment
        PTP_Segment client_fin = new PTP_Segment(false, true, last_sq_num_of_server, sq_num_client, new byte[0]);
        byte[] bin_client_fin = client_fin.convertPTPTOUDP();

        DatagramPacket udp_client_fin = new DatagramPacket(bin_client_fin, bin_client_fin.length, address, port);

        try {
            d_socket.send(udp_client_fin);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte[] receiveData=new byte[1024];
        
        // receive from server
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        try {
            d_socket.receive(receivePacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        PTP_Segment fin_ack = convertUDPTOPTP(receivePacket);

        System.out.println("------- ack from reciever in res to client fin ---------");
        System.out.println("fin bit:   " + fin_ack.getFinbit());
        System.out.println("Seq num:   " + fin_ack.getSeq_number());
        System.out.println("Ack num:   " + fin_ack.getAck_number());
        System.out.println("------------------------------------------------------");

        last_sq_num_of_server = fin_ack.getSeq_number();


        try {
            d_socket.receive(receivePacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        PTP_Segment server_fin = convertUDPTOPTP(receivePacket);

        System.out.println("------- fin from server ---------");
        System.out.println("fin bit:   " + server_fin.getFinbit());
        System.out.println("Seq num:   " + server_fin.getSeq_number());
        System.out.println("Ack num:   " + server_fin.getAck_number());
        System.out.println("------------------------------------------------------");

        last_sq_num_of_server = fin_ack.getSeq_number();


        PTP_Segment client_ack = new PTP_Segment(false, true, sq_num_client+1, last_sq_num_of_server + 1, new byte[0]);
        byte[] bin_client_ack = client_ack.convertPTPTOUDP();

        DatagramPacket udp_client_ack = new DatagramPacket(bin_client_ack, bin_client_ack.length, address, port);

        try {
            d_socket.send(udp_client_ack);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
}