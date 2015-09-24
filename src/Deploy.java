import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * Created by duncan on 24/9/15.
 */
public class Deploy extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        ADBChooser chooser = new ADBChooser(e.getDataContext());
        chooser.setTitle("Android Host");
        chooser.pack();
        chooser.setVisible(true);
    }
}
