package model;

import java.net.InetAddress;

/**
 * Created by duncan on 29/9/15.
 */
public class Server {
    private InetAddress address;
    private int port;

    public Server(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getIP() {
        return address.getHostAddress();
    }

    public String getHostName() {
        return address.getHostName();
    }

    public int getPort() {
        return port;
    }
}
