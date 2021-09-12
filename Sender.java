import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Sender class represents the sender which sends the file to the reciever
 */
public class Sender {


    public static void main (String[] args) {

        InetAddress receiver_host;
        try {
            receiver_host = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {

            System.out.println("Exception: Reciever host not initialised");
            return;
        }

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

    }

    public static void handshake(DatagramSocket socket, InetAddress address, Integer port){

        System.out.println("---- Handshake ----");
        
    }
}