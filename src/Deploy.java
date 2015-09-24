import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * Created by duncan on 24/9/15.
 */
public class Deploy extends AnAction {
    public static String URL = "http://localhost:8000";

    public void actionPerformed(AnActionEvent e) {
        ADBChooser chooser = new ADBChooser(e.getDataContext());
        chooser.setTitle("Deploy to Android Host");
        chooser.pack();
        chooser.setSize(500, 400);
        chooser.setLocationRelativeTo(null);
        chooser.setVisible(true);
    }
}
