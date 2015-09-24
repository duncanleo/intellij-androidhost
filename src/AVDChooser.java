import com.google.gson.Gson;
import model.ADBDevice;
import model.AVD;
import org.apache.commons.httpclient.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AVDChooser extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList listAVD;
    private JLabel labelStatus;
    private List<AVD> devices;

    public AVDChooser() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new Thread(() -> {
            //Get data
            org.apache.http.client.HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(Deploy.URL + "/listavd");
            request.addHeader("User-Agent", "IntelliJ-AndroidHost-Plugin");

            final List<String> items = new ArrayList<>();

            try {
                HttpResponse response = client.execute(request);
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));

                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                AVD[] avdDevices = new Gson().fromJson(result.toString(), AVD[].class);
                devices = Arrays.asList(avdDevices);
                for (AVD avd : avdDevices) {
                    String html = "<html>";
                    html += String.format("%s %s<br>", avd.getManufacturer(), avd.getDevice());
                    html += String.format("%s %s<br>", avd.getTarget(), avd.getArch());
                    html += "</html>";
                    items.add(html);
                }
                SwingUtilities.invokeLater(() -> {
                    listAVD.setListData(items.toArray());
                    labelStatus.setText("Loaded");
                    if (devices.size() > 0) {
                        listAVD.setSelectedIndex(0);
                    }
                });
            } catch (Exception e) {
                labelStatus.setText("Error: " + e.getMessage());
            }
        }).start();
    }

    private void onOK() {
// add your code here
        if (listAVD.getSelectedIndex() == -1) {
            return;
        }
        String avdName = devices.get(listAVD.getSelectedIndex()).getName();
        new Thread(() -> {
            org.apache.http.client.HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(Deploy.URL + "/start?name=" + avdName);
            request.addHeader("User-Agent", "IntelliJ-AndroidHost-Plugin");
            try {
                client.execute(request);
                SwingUtilities.invokeLater(() -> labelStatus.setText("Started " + avdName));
            } catch (Exception e) {
                labelStatus.setText("Error: " + e.getMessage());
            }
        }).start();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }
}
