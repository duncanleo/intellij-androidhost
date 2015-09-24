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
import java.util.List;

public class ADBChooser extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonStartAVD;
    private JList listADB;
    private JLabel labelStatus;
    private VirtualFile projectDir;
    private String URL = "http://localhost:8000";

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

        listADB.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listADB.setLayoutOrientation(JList.VERTICAL);

        Thread thread = new Thread(() -> {
            //Get data
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(URL + "/listadb");
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
                for (ADBDevice ad : adbDevices) {
//                        items.add(String.format("<html><font color=green>%s</font></html>", ad.getId()));
                    items.add(ad.getId());
                }
                SwingUtilities.invokeLater(() -> {
                    listADB.setListData(items.toArray());
                    labelStatus.setText("Loaded");
                    if (items.size() > 0) {
                        listADB.setSelectedIndex(0);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

    }

    private void onOK() {
// add your code here
        //TODO: Invert condition
        if (projectDir != null) {
            String path = Paths.get(projectDir.getPath(), "app", "build", "outputs", "apk").toString();
            File debugAPK = new File(path, "app-debug.apk");
            if (debugAPK.exists()) {
                new Thread(() -> {
                    try {
                        HttpClient client = HttpClientBuilder.create().build();
                        HttpPost post = new HttpPost(URL + "/install?name=" + listADB.getSelectedValue());
                        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                        builder.addBinaryBody("file", debugAPK, ContentType.APPLICATION_OCTET_STREAM, "file.apk");

                        HttpEntity multipart = builder.build();

                        ProgressHttpEntityWrapper.ProgressCallback progressCallback = progress -> {
                            //Use the progress
                            SwingUtilities.invokeLater(() -> labelStatus.setText(String.format("Uploading, %.2f%% complete...", progress)));
                            if (progress == 100.0f) {
                                dispose();
                            }
                        };

                        post.setEntity(new ProgressHttpEntityWrapper(multipart, progressCallback));
                        client.execute(post);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
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
