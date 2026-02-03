import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import quartz.HandlerJob;
import quartz.QuartzManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.List;

public abstract class BaseMarketWindow {
    protected JPanel mPanel;
    protected JBTable table;
    protected JLabel refreshTimeLabel;
    protected BaseRefreshHandler handler;
    protected String name;
    protected ConfigManager configManager;

    public BaseMarketWindow(String name) {
        this.name = name;
        this.configManager = ConfigManager.getInstance();
    }

    public JPanel getmPanel() {
        return mPanel;
    }

    public void init() {
        initUIComponents();
        setupTableEvents();
        createToolbar();
        apply();
    }

    protected void initUIComponents() {
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setToolTipText("最后刷新时间");
        refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
        table = new JBTable();
    }

    protected void setupTableEvents() {
        // 记录列名的变化
        table.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                StringBuilder tableHeadChange = new StringBuilder();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    tableHeadChange.append(table.getColumnName(i)).append(",");
                }
                ConfigManager.getInstance().saveValue(getTableHeaderKey(), tableHeadChange
                        .substring(0, tableHeadChange.length() > 0 ? tableHeadChange.length() - 1 : 0));
            }
        });
    }

    protected void createToolbar() {
        AnActionButton refreshAction = new AnActionButton("停止刷新当前表格数据", AllIcons.Actions.Pause) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                stop();
                this.setEnabled(false);
            }
        };

        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraAction(new AnActionButton("持续刷新当前表格数据", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        refresh();
                        refreshAction.setEnabled(true);
                    }
                })
                .addExtraAction(refreshAction)
                .setToolbarPosition(ActionToolbarPosition.TOP);

        JPanel toolPanel = toolbarDecorator.createPanel();
        toolbarDecorator.getActionsPanel().add(refreshTimeLabel, BorderLayout.EAST);
        toolPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        mPanel.add(toolPanel, BorderLayout.CENTER);
    }

    public void apply() {
        if (handler != null) {
            handler = createHandler();
            handler.setStriped(configManager.isTableStriped());
            handler.clearRow();
            handler.setupTable(loadCodes());
            refresh();
        }
    }

    public void refresh() {
        if (handler != null) {
            handler.refreshColorful(configManager.isColorfulEnabled());
            List<String> codes = loadCodes();
            if (CollectionUtils.isEmpty(codes)) {
                stop(); // 如果没有数据则不需要启动时钟任务浪费资源
            } else {
                handler.handle(codes);
                QuartzManager quartzManager = QuartzManager.getInstance(name);
                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put(HandlerJob.KEY_HANDLER, handler);
                dataMap.put(HandlerJob.KEY_CODES, codes);
                String cronExpression = ConfigManager.getInstance().getFundCronExpression();
                if (name.equals("Stock")) {
                    cronExpression = ConfigManager.getInstance().getStockCronExpression();
                } else if (name.equals("Coin")) {
                    cronExpression = ConfigManager.getInstance().getCoinCronExpression();
                }
                if (StringUtils.isEmpty(cronExpression)) {
                    cronExpression = getDefaultCronExpression();
                }
                quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
            }
        }
    }

    public void stop() {
        QuartzManager.getInstance(name).stopJob();
        if (handler != null) {
            handler.stopHandle();
        }
    }

    protected abstract BaseRefreshHandler createHandler();
    protected abstract List<String> loadCodes();
    protected abstract String getTableHeaderKey();
    protected abstract String getDefaultCronExpression();
}
