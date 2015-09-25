import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import model.ADBDevice;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ADBChooser extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonStartAVD;
    private JList listADB;
    private JLabel labelStatus;
    private JButton buttonRefresh;
    private VirtualFile projectDir;
    private List<ADBDevice> devices;
    private ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(2);

    public ADBChooser(DataContext context) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        try {
            projectDir = ((Project)context.getData(DataConstants.PROJECT)).getBaseDir();
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        buttonStartAVD.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AVDChooser avdChooser = new AVDChooser();
                avdChooser.setTitle("Start AVD");
                avdChooser.pack();
                avdChooser.setSize(500, 400);
                avdChooser.setLocationRelativeTo(null);
                avdChooser.setVisible(true);
            }
        });

        buttonRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadData();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                super.windowGainedFocus(e);
                loadData();
            }

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

        listADB.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listADB.setLayoutOrientation(JList.VERTICAL);

        loadData();

    }

    private void loadData() {
        new Thread(() -> {
            //Get data
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(Deploy.URL + "/listadb");
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
                ADBDevice[] adbDevices = new Gson().fromJson(result.toString(), ADBDevice[].class);
                devices = Arrays.asList(adbDevices);
                for (ADBDevice ad : adbDevices) {
                    String html = "<html>";
                    html += String.format("%s<br>", ad.getId());
                    html += String.format("%s<br>", ad.getStatus());
                    html += String.format("%s<br>", ad.getProperties());
                    html += "</html>";

                    items.add(html);
                }
                SwingUtilities.invokeLater(() -> {
                    listADB.setListData(items.toArray());
                    labelStatus.setText("Loaded");
                    if (devices.size() > 0) {
                        listADB.setSelectedIndex(0);
                    }
                });
            } catch (Exception e) {
                labelStatus.setText("Error: " + e.getMessage());
            }
        }).start();
    }

    private void onOK() {
// add your code here
        if (listADB.getSelectedIndex() == -1) {
            return;
        }
        //TODO: Invert condition
        if (projectDir != null) {
            String path = Paths.get(projectDir.getPath(), "app", "build", "outputs", "apk").toString();
            File debugAPK = new File(path, "app-debug.apk");
            if (debugAPK.exists()) {
                for (int i : listADB.getSelectedIndices()) {
                    poolExecutor.schedule(() -> {
                        try {
                            HttpClient client = HttpClientBuilder.create().build();
                            String id = devices.get(i).getId();
                            HttpPost post = new HttpPost(Deploy.URL + "/install?name=" + id);
                            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                            builder.addBinaryBody("file", debugAPK, ContentType.APPLICATION_OCTET_STREAM, "file.apk");

                            HttpEntity multipart = builder.build();

                            ProgressHttpEntityWrapper.ProgressCallback progressCallback = progress -> {
                                //Use the progress
                                SwingUtilities.invokeLater(() -> labelStatus.setText(String.format("Uploading to '%s', %.2f%% complete...", id, progress)));
                                if (i + 1 == listADB.getSelectedIndices().length && progress == 100.0f) {
                                    dispose();
                                }
                            };

                            post.setEntity(new ProgressHttpEntityWrapper(multipart, progressCallback));
                            client.execute(post);
                        } catch (Exception e) {
                            labelStatus.setText("Error: " + e.getMessage());
                        }
                    }, 100, TimeUnit.MILLISECONDS);
                }
            } else {
                JOptionPane.showMessageDialog(null, String.format("Debug apk could not be found in '%s'", debugAPK.getAbsolutePath()));
            }
        } else {
            JOptionPane.showMessageDialog(null, "Project directory could not be obtained, deployment cancelled.");
        }
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }
}
