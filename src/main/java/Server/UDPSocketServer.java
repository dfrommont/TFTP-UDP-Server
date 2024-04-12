package Server;

import java.io.*;
import java.net.*;

public class UDPSocketServer extends Thread {
    private static final int MAX_PACKET_SIZE = 10000; //Max size of file

    @Override public void run() {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(1234); //Open a socket on port 1234 (same as client)

            while (true) {
                byte[] receivedData = new byte[MAX_PACKET_SIZE];
                DatagramPacket received = new DatagramPacket(receivedData, MAX_PACKET_SIZE);
                socket.receive(received); //Copy a received packet into a byte array

                InetAddress address = received.getAddress();
                int port = received.getPort();

                String request = new String(received.getData(), 0, received.getLength()); //Pull request from data

                if (request.startsWith("GET ")) { //Client has requested file
                    try {
                        String file_name = request.substring(4); //Extract name from data

                        File file = new File("./files/" + file_name);
                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] fileData = new byte[(int) file.length()];
                        fileInputStream.read(fileData);
                        fileInputStream.close(); //Convert file in 'files/' to a byte array if it can be found

                        DatagramPacket sent = new DatagramPacket(fileData, fileData.length, address, port);
                        socket.send(sent); //Send file to client

                        received = new DatagramPacket(receivedData, receivedData.length);
                        socket.receive(received); //Receive acknowledgement from client

                        String ackMessage = new String(received.getData(), 0, received.getLength());
                        System.out.println(socket.getInetAddress() + " acknowledgment: " + ackMessage); //Output acknowledgement on Server
                    } catch (Exception error) {
                        System.err.println(error);
                    }
                } else if (request.startsWith("PUT ")) { //User has sent Server a file
                    try {
                        String fileName = request.substring(4); //Extract name from request

                        DatagramPacket dataPacket = new DatagramPacket(receivedData, receivedData.length);
                        socket.receive(new DatagramPacket(receivedData, receivedData.length)); //Receive file from Client

                        new FileOutputStream("./files/" + fileName).write(dataPacket.getData(), 0, dataPacket.getLength()); //Write data to new file

                        String ackMessage = fileName + " received successfully";
                        byte[] sendData = ackMessage.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(sendData, sendData.length, address, port);
                        socket.send(ackPacket); //Sent client acknowledgement
                        System.out.println("Server has successfully received the file: " + fileName + " from " + ackPacket.getAddress() + " (Client)");
                    } catch(Exception error) {
                        System.err.println(error);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (socket != null) socket.close();
    }

    public static void main(String[] args) {
        System.out.println("UDP Server");
        new UDPSocketServer().start(); //Call the server
    }
}