import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;

/**
 * Created by duncan on 24/9/15.
 */
public class Deploy extends AnAction {
    public static String URL;
    public static DataContext dataContext;

    public void actionPerformed(AnActionEvent e) {
        dataContext = e.getDataContext();
        ServerChooser chooser = new ServerChooser();
        chooser.setTitle("Deploy to Android Host");
        chooser.pack();
        chooser.setSize(500, 400);
        chooser.setLocationRelativeTo(null);
        chooser.setVisible(true);
    }


}
