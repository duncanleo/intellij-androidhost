import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import model.ADBDevice;
import model.APK;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
    private JComboBox comboBoxAPK;
    private VirtualFile projectDir;
    private List<ADBDevice> devices;
    private List<APK> apkList = new ArrayList<>();
    private ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(2);

    public ADBChooser() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        try {
            projectDir = ((Project)Deploy.dataContext.getData(DataConstants.PROJECT)).getBaseDir();
        } catch (Exception e) {
            e.printStackTrace();
        }

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        buttonStartAVD.addActionListener(e -> {
            AVDChooser avdChooser = new AVDChooser();
            avdChooser.setTitle("Start AVD");
            avdChooser.pack();
            avdChooser.setSize(500, 400);
            avdChooser.setLocationRelativeTo(null);
            avdChooser.setVisible(true);
        });

        buttonRefresh.addActionListener(e -> loadData());

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
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        listADB.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listADB.setLayoutOrientation(JList.VERTICAL);

        loadAPK();
        loadData();

    }

    private void loadAPK() {
        apkList.clear();
        try {
            Files.walkFileTree(Paths.get(projectDir.getPath()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith("apk")) {
                        SwingUtilities.invokeLater(() -> {
                            APK a = new APK(file.toAbsolutePath().toString());
                            apkList.add(a);
                            comboBoxAPK.addItem(a);
                        });
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error walking file tree: " + e.getMessage());
        }
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
            APK apk = apkList.get(comboBoxAPK.getSelectedIndex());
            File apkFile = new File(apk.getPath());
            if (apkFile.exists()) {
                for (int i : listADB.getSelectedIndices()) {
                    poolExecutor.schedule(() -> {
                        try {
                            HttpClient client = HttpClientBuilder.create().build();
                            String id = devices.get(i).getId();
                            HttpPost post = new HttpPost(Deploy.URL + "/install?name=" + id);
                            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                            builder.addBinaryBody("file", apkFile, ContentType.APPLICATION_OCTET_STREAM, "file.apk");

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
                JOptionPane.showMessageDialog(null, String.format("Debug apk could not be found in '%s'.\nHave you built a debug APK yet?", apkFile.getAbsolutePath()));
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
