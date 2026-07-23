import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;
import handler.TianTianFundHandler;
import cn.hutool.core.collection.CollectionUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.HttpClientPool;
import utils.LogUtil;
import utils.PopupsUiUtil;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class FundWindow implements ToolWindowFactory {
    public static final String NAME = "Fund";
    private static final String COST_COLUMN_NAME = "持仓成本价";
    private static final String BONDS_COLUMN_NAME = "持有份额";
    private static final String NOW_COLUMN_NAME = "估算净值";
    private static final String INCOME_PERCENT_COLUMN_NAME = "收益率";
    private static final String INCOME_COLUMN_NAME = "收益";
    private JPanel mPanel;

    static TianTianFundHandler fundRefreshHandler;

    private StockWindow stockWindow = new StockWindow();
    private CoinWindow coinWindow = new CoinWindow();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        //先加载代理
        loadProxySetting();

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mPanel, NAME, false);
        //股票
        Content content_stock = contentFactory.createContent(stockWindow.getmPanel(), StockWindow.NAME, false);
        //虚拟货币
        Content content_coin = contentFactory.createContent(coinWindow.getmPanel(), CoinWindow.NAME, false);
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);
        contentManager.addContent(content_stock);
        contentManager.addContent(content_coin);
        if (ConfigManager.getInstance().getFundCodes().isEmpty()) {
            // 没有配置基金数据，选择展示股票
            contentManager.setSelectedContent(content_stock);
        }
        LogUtil.setProject(project);
//        ((ToolWindowManagerEx) ToolWindowManager.getInstance(project)).addToolWindowManagerListener(new ToolWindowManagerListener() {
//            @Override
//            public void stateChanged() {
//                if (toolWindow.isVisible()){
//                    fundRefreshHandler.handle(loadFunds());
//                }
//            }
//        });
    }

    private void loadProxySetting() {
        String proxyStr = ConfigManager.getInstance().getProxySetting();
        HttpClientPool.getHttpClient().buildHttpClient(proxyStr);
    }

    @Override
    public void init(ToolWindow window) {
        // 重要：由于idea项目窗口可多个，导致FundWindow#init方法被多次调用，出现UI和逻辑错误(bug #53)，故加此判断解决
        if (Objects.nonNull(fundRefreshHandler)) {
            LogUtil.info("Leeks UI已初始化");
            return;
        }

        JLabel refreshTimeLabel = new JLabel();
        refreshTimeLabel.setToolTipText("最后刷新时间");
        refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
        JBTable table = new JBTable();
        //记录列名的变化
        table.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                WindowUtils.saveColumnWidths(table, WindowUtils.FUND_TABLE_HEADER_KEY, WindowUtils.FUND_TABLE_WIDTH_KEY);
            }

        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row < 0 || column < 0) {
                    return;
                }
                String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row), fundRefreshHandler.codeColumnIndex));//FIX 移动列导致的BUG
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    table.editCellAt(row, column);
                    Component editor = table.getEditorComponent();
                    if (editor != null) {
                        if (editor instanceof JTextField
                                && "--".equals(((JTextField) editor).getText())) {
                            ((JTextField) editor).setText("");
                        }
                        editor.requestFocus();
                    }
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                    // 鼠标左键双击
                    try {
                        PopupsUiUtil.showImageByFundCode(code, PopupsUiUtil.FundShowType.gsz, new Point(e.getXOnScreen(), e.getYOnScreen()));
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                        LogUtil.info(ex.getMessage());
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    //鼠标右键
                    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PopupsUiUtil.FundShowType>("",
                            PopupsUiUtil.FundShowType.values()) {
                        @Override
                        public @NotNull String getTextFor(PopupsUiUtil.FundShowType value) {
                            return value.getDesc();
                        }

                        @Override
                        public @Nullable PopupStep onChosen(PopupsUiUtil.FundShowType selectedValue, boolean finalChoice) {
                            try {
                                if (selectedValue == PopupsUiUtil.FundShowType.delete) {
                                    deleteFund(table, code);
                                } else {
                                    PopupsUiUtil.showImageByFundCode(code, selectedValue, new Point(e.getXOnScreen(), e.getYOnScreen()));
                                }
                            } catch (MalformedURLException ex) {
                                ex.printStackTrace();
                                LogUtil.info(ex.getMessage());
                            }
                            return super.onChosen(selectedValue, finalChoice);
                        }
                    }).show(RelativePoint.fromScreen(new Point(e.getXOnScreen(), e.getYOnScreen())));
                }
            }
        });
        fundRefreshHandler = new TianTianFundHandler(table, refreshTimeLabel);
        bindEditPersistenceListener(table);
        bindRowActions(table);
        bindGlobalKeyListener(table);
        implementRowDragAndDrop(table);
        AnActionButton refreshAction = new AnActionButton("停止刷新当前表格数据", AllIcons.Actions.Pause) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                stop();
                this.setEnabled(false);
            }
        };
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraAction((AnAction) new AnActionButton("持续刷新当前表格数据", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        refresh();
                        refreshAction.setEnabled(true);
                    }
                })
                .addExtraAction((AnAction) refreshAction)
                .setToolbarPosition(ActionToolbarPosition.TOP);
        JPanel toolPanel = toolbarDecorator.createPanel();
        toolbarDecorator.getActionsPanel().add(refreshTimeLabel, BorderLayout.EAST);
        toolPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        mPanel.add(toolPanel, BorderLayout.CENTER);
        apply();
        WindowUtils.restoreColumnWidths(table, WindowUtils.FUND_TABLE_WIDTH_KEY);
    }

    private static List<String> loadFunds() {
        List<String> funds = SettingsWindow.getConfigList(ConfigKeys.KEY_FUNDS);
        StringBuilder normalizedFunds = new StringBuilder();
        boolean changed = false;
        for (int index = 0; index < funds.size(); index++) {
            String fund = funds.get(index);
            String normalizedFund = normalizeFundConfig(fund);
            normalizedFunds.append(normalizedFund).append(";");
            changed |= !normalizedFund.equals(fund);
            funds.set(index, normalizedFund);
        }
        if (changed) {
            PropertiesComponent.getInstance().setValue(ConfigKeys.KEY_FUNDS, normalizedFunds.toString());
        }
        return funds;
    }

    private static String normalizeFundConfig(String fundConfig) {
        String code = StringUtils.substringBefore(fundConfig, ",").trim();
        if (code.matches("\\D+\\d{6}")) {
            int configSeparator = fundConfig.indexOf(',');
            return code.substring(code.length() - 6) + (configSeparator >= 0 ? fundConfig.substring(configSeparator) : "");
        }
        return fundConfig;
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return true;
    }

    public static void apply() {
        if (fundRefreshHandler != null) {
            ConfigManager configManager = ConfigManager.getInstance();
            fundRefreshHandler.setStriped(configManager.isTableStriped());
            fundRefreshHandler.setupTable(loadFunds());
            refresh();
        }
    }

    public static void refresh() {
        if (fundRefreshHandler != null) {
            ConfigManager configManager = ConfigManager.getInstance();
            boolean colorful = configManager.isColorfulEnabled();
            fundRefreshHandler.refreshColorful(colorful);
            List<String> codes = loadFunds();
            if (CollectionUtil.isEmpty(codes)) {
                stop(); //如果没有数据则不需要启动时钟任务浪费资源
            } else {
                fundRefreshHandler.handle(codes);
                QuartzManager quartzManager = QuartzManager.getInstance(NAME); // 时钟任务
                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put(HandlerJob.KEY_HANDLER, fundRefreshHandler);
                dataMap.put(HandlerJob.KEY_CODES, codes);
                String cronExpression = configManager.getFundCronExpression();
                quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
            }
        }
    }

    public static void stop() {
        QuartzManager.getInstance(NAME).stopJob();
        if (fundRefreshHandler != null) {
            fundRefreshHandler.stopHandle();
        }
    }

    private static void bindRowActions(JBTable table) {
        InputMap focusedInputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
        focusedInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteFund");
        focusedInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "moveFundUp");
        // JTable already binds Ctrl+Down while focused, so override it at the focused level.
        focusedInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "moveFundDown");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteFund");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "moveFundUp");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "moveFundDown");
        table.getActionMap().put("deleteFund", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopTableEditing(table);
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(selectedRow);
                    String code = Objects.toString(table.getModel().getValueAt(modelRow, fundRefreshHandler.codeColumnIndex), "");
                    deleteFund(table, code);
                }
            }
        });
        table.getActionMap().put("moveFundUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopTableEditing(table);
                moveFundRow(table, -1);
            }
        });
        table.getActionMap().put("moveFundDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopTableEditing(table);
                moveFundRow(table, 1);
            }
        });
    }

    private static void bindGlobalKeyListener(JBTable table) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (event.getID() != KeyEvent.KEY_PRESSED
                    || event.getKeyCode() != KeyEvent.VK_DOWN
                    || !event.isControlDown()
                    || !isFocusInsideTable(table)) {
                return false;
            }
            stopTableEditing(table);
            moveFundRow(table, 1);
            return true;
        });
    }

    private static boolean isFocusInsideTable(JBTable table) {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, table);
    }

    private static void stopTableEditing(JBTable table) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    private static void implementRowDragAndDrop(JBTable table) {
        final int[] draggedRow = {-1};
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                draggedRow[0] = table.rowAtPoint(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggedRow[0] = -1;
            }
        });
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int targetRow = table.rowAtPoint(e.getPoint());
                if (draggedRow[0] >= 0 && targetRow >= 0 && targetRow != draggedRow[0]) {
                    swapFundRows(table, draggedRow[0], targetRow);
                    draggedRow[0] = targetRow;
                }
            }
        });
    }

    private static void moveFundRow(JBTable table, int direction) {
        int selectedRow = table.getSelectedRow();
        int targetRow = selectedRow + direction;
        if (selectedRow >= 0 && targetRow >= 0 && targetRow < table.getRowCount()) {
            swapFundRows(table, selectedRow, targetRow);
        }
    }

    private static void swapFundRows(JBTable table, int firstRow, int secondRow) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int firstModelRow = table.convertRowIndexToModel(firstRow);
        int secondModelRow = table.convertRowIndexToModel(secondRow);
        Vector<?> firstData = (Vector<?>) model.getDataVector().elementAt(firstModelRow);
        model.getDataVector().set(firstModelRow, model.getDataVector().elementAt(secondModelRow));
        model.getDataVector().set(secondModelRow, firstData);
        model.fireTableDataChanged();
        table.setRowSelectionInterval(secondRow, secondRow);
        saveFundOrder(model);
    }

    private static void deleteFund(JBTable table, String code) {
        if (StringUtils.isBlank(code)) {
            return;
        }
        StringBuilder configs = new StringBuilder();
        for (String config : SettingsWindow.getConfigList(ConfigKeys.KEY_FUNDS)) {
            if (!code.equals(StringUtils.substringBefore(config, ",").trim())) {
                configs.append(config).append(";");
            }
        }
        PropertiesComponent.getInstance().setValue(ConfigKeys.KEY_FUNDS, configs.toString());
        apply();
    }

    private static void saveFundOrder(DefaultTableModel model) {
        List<String> configs = SettingsWindow.getConfigList(ConfigKeys.KEY_FUNDS);
        StringBuilder orderedConfigs = new StringBuilder();
        for (int row = 0; row < model.getRowCount(); row++) {
            String code = Objects.toString(model.getValueAt(row, fundRefreshHandler.codeColumnIndex), "");
            for (String config : configs) {
                if (code.equals(StringUtils.substringBefore(config, ",").trim())) {
                    orderedConfigs.append(config).append(";");
                    break;
                }
            }
        }
        PropertiesComponent.getInstance().setValue(ConfigKeys.KEY_FUNDS, orderedConfigs.toString());
    }

    private static void bindEditPersistenceListener(JBTable table) {
        fundRefreshHandler.addTableModelListener(event -> {
            if (event.getType() != TableModelEvent.UPDATE || event.getColumn() < 0) {
                return;
            }
            for (int row = event.getFirstRow(); row <= event.getLastRow(); row++) {
                persistEditedCell(table, row, event.getColumn());
            }
        });
    }

    private static void persistEditedCell(JBTable table, int modelRow, int modelColumn) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (modelRow < 0 || modelRow >= model.getRowCount() || modelColumn < 0 || modelColumn >= model.getColumnCount()) {
            return;
        }
        String editedColumn = WindowUtils.remapPinYin(model.getColumnName(modelColumn));
        if (!COST_COLUMN_NAME.equals(editedColumn) && !BONDS_COLUMN_NAME.equals(editedColumn)) {
            return;
        }

        String code = normalizeValue(Objects.toString(model.getValueAt(modelRow, fundRefreshHandler.codeColumnIndex), ""));
        if (StringUtils.isBlank(code)) {
            return;
        }

        String costPrice = getValueByColumnName(model, modelRow, COST_COLUMN_NAME);
        String bonds = getValueByColumnName(model, modelRow, BONDS_COLUMN_NAME);
        PropertiesComponent instance = PropertiesComponent.getInstance();
        StringBuilder fundConfigs = new StringBuilder();
        boolean updated = false;
        for (String fundConfig : StringUtils.defaultString(instance.getValue(ConfigKeys.KEY_FUNDS)).split(";")) {
            if (StringUtils.isBlank(fundConfig)) {
                continue;
            }
            if (code.equals(StringUtils.substringBefore(fundConfig.trim(), ","))) {
                fundConfig = mergeFundConfig(code, costPrice, bonds);
                updated = true;
            }
            fundConfigs.append(fundConfig.trim()).append(";");
        }
        if (!updated) {
            fundConfigs.append(mergeFundConfig(code, costPrice, bonds)).append(";");
        }
        instance.setValue(ConfigKeys.KEY_FUNDS, fundConfigs.toString());
        refreshIncomeColumns(model, modelRow);
    }

    private static String getValueByColumnName(DefaultTableModel model, int row, String columnName) {
        int column = findColumnIndex(model, columnName);
        return column < 0 ? "" : normalizeValue(Objects.toString(model.getValueAt(row, column), ""));
    }

    private static void refreshIncomeColumns(DefaultTableModel model, int row) {
        int nowColumn = findColumnIndex(model, NOW_COLUMN_NAME);
        int costColumn = findColumnIndex(model, COST_COLUMN_NAME);
        int bondsColumn = findColumnIndex(model, BONDS_COLUMN_NAME);
        int incomePercentColumn = findColumnIndex(model, INCOME_PERCENT_COLUMN_NAME);
        int incomeColumn = findColumnIndex(model, INCOME_COLUMN_NAME);
        if (nowColumn < 0 || costColumn < 0 || bondsColumn < 0 || incomePercentColumn < 0 || incomeColumn < 0) {
            return;
        }

        String incomePercent = "--%";
        String income = "--";
        try {
            String nowValue = normalizeValue(Objects.toString(model.getValueAt(row, nowColumn), ""));
            String costPrice = normalizeValue(Objects.toString(model.getValueAt(row, costColumn), ""));
            String bonds = normalizeValue(Objects.toString(model.getValueAt(row, bondsColumn), ""));
            if (StringUtils.isNotBlank(nowValue) && StringUtils.isNotBlank(costPrice)) {
                BigDecimal now = new BigDecimal(nowValue);
                BigDecimal cost = new BigDecimal(costPrice);
                BigDecimal incomeDifference = now.subtract(cost);
                incomePercent = cost.compareTo(BigDecimal.ZERO) <= 0 ? "0%" : incomeDifference.divide(cost, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(3, RoundingMode.HALF_UP).toPlainString() + "%";
                if (StringUtils.isNotBlank(bonds)) {
                    income = incomeDifference.multiply(new BigDecimal(bonds)).setScale(2, RoundingMode.HALF_UP).toPlainString();
                }
            }
        } catch (NumberFormatException ignored) {
            // Keep placeholders for incomplete or invalid input.
        }
        model.setValueAt(incomePercent, row, incomePercentColumn);
        model.setValueAt(income, row, incomeColumn);
    }

    private static int findColumnIndex(DefaultTableModel model, String columnName) {
        for (int column = 0; column < model.getColumnCount(); column++) {
            if (columnName.equals(WindowUtils.remapPinYin(model.getColumnName(column)))) {
                return column;
            }
        }
        return -1;
    }

    private static String mergeFundConfig(String code, String costPrice, String bonds) {
        if (StringUtils.isBlank(costPrice) && StringUtils.isBlank(bonds)) {
            return code;
        }
        return code + "," + costPrice + "," + bonds;
    }

    private static String normalizeValue(String value) {
        String normalized = StringUtils.trimToEmpty(value);
        return "--".equals(normalized) ? "" : normalized;
    }
}
