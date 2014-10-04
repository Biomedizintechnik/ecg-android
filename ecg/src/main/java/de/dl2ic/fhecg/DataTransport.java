package de.dl2ic.fhecg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class DataTransport extends Thread {
    private DatagramSocket socket;
    private BlockingQueue<byte[]> queue;

    private String host;
    private int port;

    public DataTransport() {
        queue = new LinkedBlockingQueue<byte[]>();
    }

    public void connect(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void put(byte[] packet) {
        queue.add(packet);
    }

    public void run() {
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        try {
            socket =  new DatagramSocket(null);
            socket.setReuseAddress(true);
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            byte[] data;
            try {
                data = queue.take();
            }
            catch (InterruptedException e) {
                continue;
            }

            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
