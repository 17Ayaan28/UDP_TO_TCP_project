import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

public class Receiver {

    public static int last_seq_num = 1;
    public static void main(String[] args) throws IOException {
        int receiver_port = Integer.parseInt(args[0]);
        String file_received = args[1];

        System.out.println("Receiver Port: " + receiver_port);
        System.out.println("Receiving file name:  " + file_received);

        File outputFile = new File(file_received);

        ArrayList<Byte> file_data = new ArrayList<Byte>();

        DatagramSocket serverSocket = new DatagramSocket(receiver_port);

        System.out.println("Server is ready : ");
        
        while (true) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            serverSocket.receive(receivePacket);
            /*
            for(int i = 0; i < receivePacket.getData().length; i++){
                file_data.add(receivePacket.getData()[i]);
            }
            */

            System.out.println("Received packet length:  " + receivePacket.getLength());
            
            PTP_Segment p = convertUDPTOPTP(receivePacket);
            
            System.out.println("------- Received PTP Packet ---------");
            System.out.println("Sequence Number: " + p.getSeq_number());
            System.out.println("Synbit:  " + p.getSynbit());
            System.out.println("ack number:  " + p.getAck_number());
            System.out.println("payload size:  " + receivePacket.getLength());
            System.out.println("-------");

            if (receivePacket.getLength() > 10) {
                for (int i = 11; i <= receivePacket.getLength(); i++) {
                    file_data.add(receivePacket.getData()[i]);
                }
            }

            if (p.getSynbit() == 1) {

                System.out.println("Section 1");
                PTP_Segment syn_ack = new PTP_Segment(true, false, last_seq_num, p.getSeq_number() + 1, new byte[0]);
                last_seq_num++;
                byte[] bin_syn_ack = syn_ack.convertPTPTOUDP();

                DatagramPacket d_p = new DatagramPacket(bin_syn_ack, bin_syn_ack.length, receivePacket.getSocketAddress());

                serverSocket.send(d_p);
            } else {

                if (p.getFinbit() == 1) {

                    System.out.println("--- Termination at server begins ------");
                    System.out.println("sever's ack for client fin");

                    PTP_Segment fin_ack = new PTP_Segment(false, false, p.getAck_number(), p.getSeq_number() + 1, new byte[0]);
                    last_seq_num++;
                    byte[] bin_syn_ack = fin_ack.convertPTPTOUDP();

                    DatagramPacket d_p_1 = new DatagramPacket(bin_syn_ack, bin_syn_ack.length, receivePacket.getSocketAddress());

                    serverSocket.send(d_p_1);

                    ///-----

                    PTP_Segment server_fin = new PTP_Segment(false, true, last_seq_num, p.getSeq_number() + 1, new byte[0]);
                    last_seq_num++;
                    byte[] bin_server_fin = server_fin.convertPTPTOUDP();

                    DatagramPacket d_p_2 = new DatagramPacket(bin_server_fin, bin_server_fin.length, receivePacket.getSocketAddress());

                    serverSocket.send(d_p_2);

                    //--------

                    serverSocket.receive(receivePacket);
                    PTP_Segment client_ack = convertUDPTOPTP(receivePacket);

                    System.out.println("----client ack ------");
                    System.out.println("Termination complete, closing server");

                    byte[] f_d_bt = new byte[file_data.size()];

                    for(int i = 0; i < file_data.size(); i++) {
                        f_d_bt[i] = file_data.get(i).byteValue();
                    }

                    FileOutputStream fos = null;

                    fos = new FileOutputStream(outputFile);
                    fos.write(f_d_bt);
                    fos.close();

                    break;
                } else {

                    System.out.println("Section 2");
                    PTP_Segment ack_segment = new PTP_Segment(false, false, p.getAck_number(), p.getSeq_number() + receivePacket.getLength() - 10, new byte[0]);  // might need to add MSS to sequence number
                    last_seq_num++;  
                    byte[] bin_ack_segment = ack_segment.convertPTPTOUDP();
    
                    DatagramPacket d_p = new DatagramPacket(bin_ack_segment, bin_ack_segment.length, receivePacket.getSocketAddress());
    
                    serverSocket.send(d_p);
                }

            }

        }
        serverSocket.close();
        System.out.println("-----Socket closed-------");
        
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


}

