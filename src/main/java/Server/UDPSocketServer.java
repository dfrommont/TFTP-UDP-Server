package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

public class UDPSocketServer extends Thread {

    protected DatagramSocket socket = null;

    public UDPSocketServer() throws SocketException {
        this("UDPSocketServer");
    }

    public UDPSocketServer(String name) throws SocketException {
        super(name);
        // **********************************************
        // Add a line here to instantiate a DatagramSocket for the socket field defined above.
        // Bind the socket to port 9000 (any port over 1024 would be ok as long as no other application uses it).
        // Ports below 1024 require administrative rights when running the applications.
        // Take a note of the port as the client needs to send its datagram to an IP address and port to which this server socket is bound.
        //***********************************************

        socket = new DatagramSocket(1234);

    }

    @Override
    public void run() {

        byte[] recvBuf = new byte[256];     // a byte array that will store the data received by the client

        try {
            DatagramPacket packet = new DatagramPacket(recvBuf, 256);
            socket.receive(packet);

            String dString = new Date().toString();
            int len = dString.length();                                             // length of the byte array
            byte[] buf = new byte[len];                                             // byte array that will store the data to be sent back to the client
            System.arraycopy(dString.getBytes(), 0, buf, 0, len);


            InetAddress addr = packet.getAddress();
            int srcPort = packet.getPort();

            packet.setData(buf);

            packet.setAddress(addr);
            packet.setPort(srcPort);


            socket.send(packet);
        } catch (IOException e) {
            System.err.println(e);
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        //new UDPSocketServer().start();
        System.out.println("UDP Socket Server");
        Scanner input = new Scanner(System.in);
        System.out.println("1. Read a file");
        System.out.println("2. Write a file");
        System.out.println("Enter a number: ");
        String in = input.next();
        if (Objects.equals(in, "1")) {
            //read
        } else if (Objects.equals(in, "2")) {
            //write
        } else {
            System.out.println("Please enter a valid choice");
        }
    }

}
