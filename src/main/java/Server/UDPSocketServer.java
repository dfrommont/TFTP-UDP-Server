package Server;

import java.io.*;
import java.net.*;

public class UDPSocketServer extends Thread {
    private static final int MAX_PACKET_SIZE = 10000; // Maximum size of UDP packet

    @Override public void run() {
        DatagramSocket serverSocket = null;

        try {
            // Create a UDP socket
            serverSocket = new DatagramSocket(1234);

            byte[] receiveData = new byte[MAX_PACKET_SIZE];
            byte[] sendData;

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                // Extract client's request (file name or file data)
                String request = new String(receivePacket.getData(), 0, receivePacket.getLength());

                if (request.startsWith("GET:")) {
                    try {
                        // Request to retrieve file by name
                        String fileName = request.substring(4); // Remove "GET:" prefix

                        // Read requested file
                        FileInputStream fileInputStream = new FileInputStream("./files/" + fileName);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        // Read file into buffer
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }

                        // Get file data
                        byte[] fileData = byteArrayOutputStream.toByteArray();

                        // Send file data back to client
                        DatagramPacket sendPacket = new DatagramPacket(fileData, fileData.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);

                        receiveData = new byte[MAX_PACKET_SIZE];
                        receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);

                        // Process acknowledgment
                        String ackMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println(serverSocket.getInetAddress() + " acknowledgment: " + ackMessage);
                    } catch (Exception error) {
                        System.err.println(error);
                    }
                } else if (request.startsWith("PUT:")) {
                    try {
                        // Request to receive file data from client
                        String fileName = request.substring(4); // Remove "PUT:" prefix

                        // Receive file data from client
                        DatagramPacket dataPacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(dataPacket);

                        // Write received file data to file
                        FileOutputStream fileOutputStream = new FileOutputStream("./files/" + fileName);
                        fileOutputStream.write(dataPacket.getData(), 0, dataPacket.getLength());

                        // Send acknowledgment to client
                        String ackMessage = fileName + " received successfully";
                        sendData = ackMessage.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                        serverSocket.send(ackPacket);
                        System.out.println("Server has successfully received the file: " + fileName + " from " + ackPacket.getAddress() + " (Client)");
                    } catch(Exception error) {
                        System.err.println(error);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverSocket.close();
    }

    public static void main(String[] args) throws SocketException {
        System.out.println("Server started");
        new UDPSocketServer().start();
    }
}