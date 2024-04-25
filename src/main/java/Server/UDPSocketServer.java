package Server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UDPSocketServer extends Thread {

    private static final int MAX_SIZE = 512; //Max size of a packet that can be communicated

    @Override public void run() {
        try (DatagramSocket socket = new DatagramSocket(69)) { //Open a socket on port 69

            while (true) {
                DatagramPacket receivedPacket = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                socket.receive(receivedPacket); //Receive request packet from the Client

                SocketAddress socketAddress = new InetSocketAddress(receivedPacket.getAddress(), receivedPacket.getPort()); //Pull Client address and port number from packet

                byte[] requestData = receivedPacket.getData();
                ByteBuffer requestBuffer = ByteBuffer.wrap(requestData); //Convert packet back to a byteBuffer

                short opcode = requestBuffer.getShort(); //Extract the opcode from the byteBuffer
                if (opcode == 1) {
                    sendFile(socket, socketAddress, requestBuffer); //Call method to handle sending a file if opcode is 01 - RRQ or read request
                } else if (opcode == 2) {
                    receiveFile(socket, socketAddress, requestBuffer); //Call method to handle sending a file if opcode is 02 - WRR or write request
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); //Output error message if socket could not be opened
        }
    }

    private static void sendFile(DatagramSocket socket, SocketAddress socketAddress, ByteBuffer requestBuffer) throws IOException {
        requestBuffer.getShort();
        StringBuilder stringBuilder = new StringBuilder();
        byte bytes;
        while ((bytes = requestBuffer.get()) != 0) {
            stringBuilder.append((char) bytes); //Extract the file name from the byteBuffer
        }
        String fileName = stringBuilder.toString();
        requestBuffer.getShort();

        File file = new File("./files/" + fileName); //Open the desired file
        if (!file.exists()) {
            ByteBuffer errorBuffer = ByteBuffer.allocate(5 + "File not found".length() + 1);
            errorBuffer.putShort((short) 5);
            errorBuffer.putShort((short) 1); //Error opcode
            errorBuffer.put("File not found".getBytes());
            errorBuffer.put((byte) 0);

            DatagramPacket errorPacket = new DatagramPacket(errorBuffer.array(), errorBuffer.position(), socketAddress);
            socket.send(errorPacket); //If file does not exist, send error package to Client
            return;
        }

        FileInputStream fileInputStream = new FileInputStream(file); //Open a fileInputStream for the file

        int blockNumber = 1;
        while (true) {
            byte[] data = new byte[MAX_SIZE];
            int bytesRead = fileInputStream.read(data, 4, MAX_SIZE - 4);
            if (bytesRead == -1) {
                break; //Stop if there is no more data to be read from the file
            }

            ByteBuffer responseBuffer = ByteBuffer.allocate(bytesRead + 4);
            responseBuffer.putShort((short) 3); //Data opcode
            responseBuffer.putShort((short) blockNumber);
            responseBuffer.put(Arrays.copyOfRange(data, 4, bytesRead + 4)); //Generate a DATA package for the current block

            DatagramPacket responsePacket = new DatagramPacket(responseBuffer.array(), responseBuffer.position(), socketAddress);
            socket.send(responsePacket); //Package this data into a packet and send it to the client

            DatagramPacket ackPacket = new DatagramPacket(new byte[4], 4);
            socket.receive(ackPacket); //Receive acknowledgement back from the client for the block
            blockNumber++;

            if (bytesRead < MAX_SIZE) {
                break; //Stop writing when the all the block has been sent
            }
        }

        fileInputStream.close();
    }

    private static void receiveFile(DatagramSocket socket, SocketAddress socketAddress, ByteBuffer requestBuffer) throws IOException {
        requestBuffer.getShort();
        StringBuilder stringBuilder = new StringBuilder();
        byte bytes;
        while ((bytes = requestBuffer.get()) != 0) {
            stringBuilder.append((char) bytes); //Extract desired file name from the request
        }
        String fileName = stringBuilder.toString();
        requestBuffer.getShort();

        File file = new File("./files/" + fileName);
        if (file.exists()) {
            ByteBuffer errorBuffer = ByteBuffer.allocate(5 + "File already exists".length() + 1);
            errorBuffer.putShort((short) 5);
            errorBuffer.putShort((short) 6); //Error opcode
            errorBuffer.put("File already exists".getBytes());
            errorBuffer.put((byte) 0); //Generate error package if the file being sent already exists

            DatagramPacket errorPacket = new DatagramPacket(errorBuffer.array(), errorBuffer.position(), socketAddress);
            socket.send(errorPacket); //Package the error into a packet and send this to the Client
            return;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(file); //Open a FileOutputStream on the file

        int blockNumber = 0;
        while (true) {
            DatagramPacket dataPacket = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
            socket.receive(dataPacket); //Receive a data packet from the client

            ByteBuffer dataBuffer = ByteBuffer.wrap(dataPacket.getData()); //Convert this back to a byteBuffer
            short opcode = dataBuffer.getShort();
            if (opcode != 3) {
                ByteBuffer errorBuffer = ByteBuffer.allocate(5 + "Unexpected opcode".length() + 1);
                errorBuffer.putShort((short) 5); // Error opcode
                errorBuffer.putShort((short) 4);
                errorBuffer.put("Unexpected opcode".getBytes());
                errorBuffer.put((byte) 0); //Generate error if the opcode is not 3 - DATA opcode

                DatagramPacket errorPacket = new DatagramPacket(errorBuffer.array(), errorBuffer.position(), socketAddress);
                socket.send(errorPacket); //Package this error into a packet and send back to Client
                return;
            }

            int receivedBlockNumber = dataBuffer.getShort();
            if (receivedBlockNumber != blockNumber + 1) {
                ByteBuffer errorBuffer = ByteBuffer.allocate(5 + "Unexpected opcode".length() + 1);
                errorBuffer.putShort((short) 5); // Error opcode
                errorBuffer.putShort((short) 4);
                errorBuffer.put("Incorrect block number".getBytes());
                errorBuffer.put((byte) 0); //Generate error if block within the received packet is not in order

                DatagramPacket errorPacket = new DatagramPacket(errorBuffer.array(), errorBuffer.position(), socketAddress);
                socket.send(errorPacket); //Package this error into a packet for the Client
                return;
            }

            byte[] blockData = Arrays.copyOfRange(dataPacket.getData(), 4, dataPacket.getLength());
            fileOutputStream.write(blockData);
            fileOutputStream.flush(); //Write the data contained within the packet to the file

            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
            ackBuffer.putShort((short) 4); // ACK opcode
            ackBuffer.putShort((short) receivedBlockNumber); //Generate an acknowledgement for the data block just received

            DatagramPacket ackPacket = new DatagramPacket(ackBuffer.array(), ackBuffer.position(), socketAddress);
            socket.send(ackPacket); //Send this acknowledgement to the Client in a packet

            blockNumber++;

            if (dataPacket.getLength() < MAX_SIZE) {
                break;
            }
        }
        fileOutputStream.close();
    }


    public static void main(String[] args) {
        System.out.println("UDP Server");
        new UDPSocketServer().start(); //Start the UDP Server using threads
    }
}
