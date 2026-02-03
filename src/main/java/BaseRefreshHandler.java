import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.util.List;

public abstract class BaseRefreshHandler {
    protected JBTable table;
    protected JLabel refreshTimeLabel;
    protected boolean striped;
    protected int codeColumnIndex;

    public BaseRefreshHandler(JBTable table, JLabel refreshTimeLabel) {
        this.table = table;
        this.refreshTimeLabel = refreshTimeLabel;
    }

    public void setStriped(boolean striped) {
        this.striped = striped;
    }

    public abstract void clearRow();
    public abstract void setupTable(List<String> codes);
    public abstract void handle(List<String> codes);
    public abstract void stopHandle();
    public abstract void refreshColorful(boolean colorful);
}
