import model.Server;

import javax.swing.*;
import java.awt.event.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ServerChooser extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList listServer;
    private JButton buttonRefresh;
    private List<Server> serverList = new ArrayList<>();
    private List<String> serverListHTML = new ArrayList<>();
    private DatagramSocket clientSocket;
    private final static String PAYLOAD = "ANDROID_HOST_CLIENT_REQUEST_RECOGNITION", SERVER_RESPONSE = "ANDROID_HOST_SERVER_RECOGNISED";

    public ServerChooser() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        buttonRefresh.addActionListener(e -> getServers());

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        listServer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listServer.setLayoutOrientation(JList.VERTICAL);

        getServers();
    }

    private void getServers() {
        serverList.clear();
        serverListHTML.clear();
        //Get the servers
        new Thread(() -> {
            try {
                clientSocket = new DatagramSocket();
                InetAddress IPAddress = InetAddress.getByName("192.168.0.255");
                byte[] sendData;
                byte[] receiveData = new byte[4096];
                sendData = PAYLOAD.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8001);
                clientSocket.send(sendPacket);

                do {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    //Server will echo back the payload
                    String reply = new String(receivePacket.getData());
                    if (reply.trim().equals(SERVER_RESPONSE)) {
                        Server s = new Server(receivePacket.getAddress(), receivePacket.getPort());
                        serverList.add(s);
                        String html = "<html>";
                        html += String.format("<b>%s</b><br>", s.getHostName());
//                            html += String.format("Port %d<br>", s.getPort());
                        html += "Port 8000<br>";
                        html += "</html>";
                        serverListHTML.add(html);
                        SwingUtilities.invokeLater(() -> listServer.setListData(serverListHTML.toArray()));
                    }
                } while (!clientSocket.isClosed());
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }).start();
    }

    private void onOK() {
// add your code here
        if (listServer.getSelectedIndex() == -1) {
            return;
        }
        Deploy.URL = String.format("http://%s:8000", serverList.get(listServer.getSelectedIndex()).getIP());
        ADBChooser chooser = new ADBChooser();
        chooser.setTitle("Deploy to Android Host");
        chooser.pack();
        chooser.setSize(500, 400);
        chooser.setLocationRelativeTo(null);
        chooser.setVisible(true);
        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    @Override
    public void dispose() {
        if (clientSocket != null) {
            clientSocket.close();
        }
        super.dispose();
    }
}
