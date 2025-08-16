import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import handler.SinaStockHandler;
import handler.StockRefreshHandler;
import handler.TencentStockHandler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.LogUtil;
import utils.PopupsUiUtil;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class StockWindow {
    public static final String NAME = "Stock";

    private static String[] canEditColumnNames = new String[]{"成本价", "持仓"};
    private JPanel mPanel;

    static StockRefreshHandler handler;

    static JBTable table;
    static JLabel refreshTimeLabel;


    private JDialog searchDialog; // 搜索弹窗
    private JTextField searchField; // 搜索输入框
    private JList<String> resultList; // 搜索结果列表
    private DefaultListModel<String> listModel; // 列表模型
    private Point initialClick; // 记录初始点击位置
    static PropertiesComponent instance = PropertiesComponent.getInstance();


    public JPanel getmPanel() {
        return mPanel;
    }

    static {
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setToolTipText("最后刷新时间");
        refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
        table = new JBTable();
        //记录列名的变化
        table.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                StringBuilder tableHeadChange = new StringBuilder();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    tableHeadChange.append(table.getColumnName(i)).append(",");
                }
                PropertiesComponent instance = PropertiesComponent.getInstance();
                //将列名的修改放入环境中 key:stock_table_header_key
                instance.setValue(WindowUtils.STOCK_TABLE_HEADER_KEY, tableHeadChange.substring(0, tableHeadChange.length() > 0 ? tableHeadChange.length() - 1 : 0));

            }

        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (table.getSelectedRow() < 0) return;
                //获取股票代码
                String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), handler.codeColumnIndex));//FIX 移动列导致
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) { // 单击检测
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());

                    if (row >= 0 && col >= 0) {
                        table.editCellAt(row, col); // 手动触发编辑模式
                        Component editor = table.getEditorComponent();
                        if (editor != null) {
                            editor.requestFocus(); // 聚焦到编辑器
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    //鼠标右键
                    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PopupsUiUtil.StockShowType>("", PopupsUiUtil.StockShowType.values()) {
                        @Override
                        public @NotNull String getTextFor(PopupsUiUtil.StockShowType value) {
                            return value.getDesc();
                        }

                        @Override
                        public @Nullable PopupStep onChosen(PopupsUiUtil.StockShowType selectedValue, boolean finalChoice) {
                            try {
                                //如果选择删除
                                if (selectedValue == PopupsUiUtil.StockShowType.delete) {
                                    String key = getKeyForName(NAME);
                                    // 从 PropertiesComponent 中移除数据
                                    String storedValue = instance.getValue(key);
                                    if (StringUtils.isNotBlank(storedValue)) {
                                        String[] split = storedValue.split(";");
                                        StringBuilder codeString = new StringBuilder();
                                        for (String splitCode : split) {
                                            if (!splitCode.contains(code) && !splitCode.isEmpty()) { // 关键：只添加不包含目标字符串且不为空的项
                                                codeString.append(splitCode).append(";");
                                            }
                                        }
                                        instance.setValue(key, codeString.toString());
                                    }
                                    apply();
                                } else {
                                    PopupsUiUtil.showImageByStockCode(code, selectedValue, new Point(e.getXOnScreen(), e.getYOnScreen()));
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
//
//        // 添加 TableModelListener
//        table.getModel().addTableModelListener(e -> {
//            if (e.getType() == TableModelEvent.UPDATE) {
//                editCellValue();
//            }
//        });

//        // 添加 CellEditorListener
//        table.getDefaultEditor(Object.class).addCellEditorListener(new javax.swing.event.CellEditorListener() {
//            @Override
//            public void editingStopped(javax.swing.event.ChangeEvent e) {
//                editCellValue();
//            }
//
//            @Override
//            public void editingCanceled(javax.swing.event.ChangeEvent e) {
//            }
//        });

        // 添加 CellEditorListener，监听编辑完成事件
        TableCellEditor cellEditor = table.getDefaultEditor(Object.class);
        if (cellEditor != null) {
            cellEditor.addCellEditorListener(new CellEditorListener() {
                @Override
                public void editingStopped(ChangeEvent e) {
                    editCellValue(); // 编辑完成，保存数据
                }

                @Override
                public void editingCanceled(ChangeEvent e) {
                }
            });
        }

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "moveUp");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "moveDown");

        table.getActionMap().put("moveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRow(-1); // 向上移动
            }
        });

        table.getActionMap().put("moveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRow(1); // 向下移动
                //并配置到配置文件
            }
        });

    }

    private static void moveRow(int direction) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return; // 没有选中任何行
        }

        // 获取模型和目标行索引
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int targetRow = selectedRow + direction;

        // 确保目标行在有效范围内
        if (targetRow < 0 || targetRow >= model.getRowCount()) {
            return;
        }

        // 交换行数据
        Vector<?> rowData = (Vector<?>) model.getDataVector().elementAt(selectedRow);
        model.getDataVector().set(selectedRow, model.getDataVector().elementAt(targetRow));
        model.getDataVector().set(targetRow, rowData);

        // 通知模型数据已更改
        model.fireTableDataChanged();

        // 更新选中行
        table.setRowSelectionInterval(targetRow, targetRow);
        //更新到配置文件
        //第几行
        int row = table.getSelectedRow();
        //弹窗

        String key = getKeyForName(NAME);
        // 从 PropertiesComponent 中移除数据
        String storedValue = instance.getValue(key);

        String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), handler.codeColumnIndex));//FIX 移动列导致

        //根据;切割获取所有股票代码配置
        String[] split = storedValue.split(";");
        List<String> list = new java.util.ArrayList<>(Arrays.stream(split).toList());
        String remove = "";
        for (String s : list) {
            if (s.contains(code)) {
                remove = s;
                list.remove(s);
                break;
            }
        }
        list.add(row, remove);
        //将list重新添加到配置文件
        StringBuilder codeString = new StringBuilder();
        for (String s : list) {
            codeString.append(s).append(";");
        }
        instance.setValue(key, codeString.toString());
//        apply();
    }


    private static void editCellValue() {
        String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), handler.codeColumnIndex));//FIX 移动列导致的BUG
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row >= 0 && col >= 0) {
            //获取编辑列得名称
            String editColumnName = table.getColumnName(col);
            String costPrise = "-1";
            String bonds = "-1";
            String editValue = table.getValueAt(row, col).toString();
            if (editColumnName.equals("成本价")) {
                costPrise = editValue;
            } else if (editColumnName.equals("持仓")) {
                bonds = editValue;
            }
            //同时更新配置
            //1. 获取配置
            String key = getKeyForName(NAME);
            // 从 PropertiesComponent 中移除数据
            String storedValue = instance.getValue(key);
            //如果获取配置不为空
            if (StringUtils.isNotBlank(storedValue)) {
                //根据;切割获取所有股票代码配置
                String[] split = storedValue.split(";");
                StringBuilder codeString = new StringBuilder();
                //循环股票列表
                for (String splitCode : split) {
                    //如果股票配置包含但钱当前股票代码 , 更新股票配置
                    if (splitCode.contains(code)) {
                        String[] stockConfig = splitCode.split(",");
                        //用switch判断配置数组里面有多少元素
                        switch (stockConfig.length) {
                            case 1:
                                //如果就一个说明只有代码
                                costPrise = !costPrise.equals("-1") ? costPrise : "";
                                bonds = !bonds.equals("-1") ? bonds : "";
                                if (StringUtils.isNotBlank(costPrise) || StringUtils.isNotBlank(bonds)) {
                                    splitCode += "," + costPrise + "," + bonds;
                                }
                                break;
                            case 2:
                                //如果有两个 , 判断是价格还是持仓 , 如果下标为1得不是小数并且是整百 , 说明是持仓
                                if (StringUtils.isNumeric(stockConfig[1]) && Integer.parseInt(stockConfig[1]) % 100 == 0) {
                                    //是持仓直接更新持仓
                                    bonds = !bonds.equals("-1") ? bonds : stockConfig[1];
                                    splitCode = stockConfig[0] + "," + costPrise + "," + bonds;
                                } else {
                                    //不是持仓直接更新成本价
                                    costPrise = !costPrise.equals("-1") ? costPrise : stockConfig[1];
                                    splitCode = stockConfig[0] + "," + costPrise + "," + bonds;
                                }
                                break;
                            case 3:
                                //是持仓直接更新持仓
                                bonds = !bonds.equals("-1") ? bonds : stockConfig[2];
                                //不是持仓直接更新成本价
                                costPrise = !costPrise.equals("-1") ? costPrise : stockConfig[1];
                                splitCode = stockConfig[0] + "," + costPrise + "," + bonds;
                                break;
                            default:
                                break;
                        }

                    }
                    splitCode = splitCode.split(",").length == 1 ? splitCode.replaceAll(",", "") : splitCode;
                    codeString.append(splitCode).append(";");
                }
                instance.setValue(key, codeString.toString());
            }
        }
        apply();
    }


    public StockWindow() {
        // 切换接口
        handler = factoryHandler();

        // 初始化搜索弹窗
        initSearchDialog();

        AnActionButton refreshAction = new AnActionButton("停止刷新当前表格数据", AllIcons.Actions.Pause) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                stop();
                this.setEnabled(false);
            }
        };

        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(table).addExtraAction(new AnActionButton("持续刷新当前表格数据", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refresh();
                refreshAction.setEnabled(true);
            }
        }).addExtraAction(refreshAction).setToolbarPosition(ActionToolbarPosition.TOP);

        JPanel toolPanel = toolbarDecorator.createPanel();
        toolbarDecorator.getActionsPanel().add(refreshTimeLabel, BorderLayout.EAST);
        toolPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        mPanel.add(toolPanel, BorderLayout.CENTER);

        // 非主要tab，需要创建，创建时立即应用数据
        apply();

        // 绑定全局快捷键 F7
        bindGlobalKeyListener();
    }

    // 初始化搜索弹窗
    private void initSearchDialog() {

        // 获取屏幕的宽度和高度
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // 创建搜索弹窗
        searchDialog = new JDialog((JFrame) null, false);
        searchDialog.setUndecorated(true); // 无标题栏
        searchDialog.setSize(600, 50);    // 设置大小
        searchDialog.setLayout(new BorderLayout());
        searchDialog.setLocationRelativeTo(null); // 居中显示
        searchDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE); // 默认隐藏
        // 设置透明度
        searchDialog.setOpacity(0.85f);  // 设置透明度，范围从0.0 (完全透明) 到 1.0 (完全不透明)

        // 背景面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(0, 0, 0, 0)); // 确保背景是透明的
        searchDialog.add(contentPanel, BorderLayout.CENTER);  // 添加自定义面板到弹窗

        // 搜索输入框
        searchField = new JTextField(50);
        Font labelFont = UIUtil.getLabelFont();
        searchField.setFont(labelFont);
        searchField.setBackground(JBColor.background()); // 搜索框背景颜色跟随系统主题
        searchField.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        searchField.setPreferredSize(new Dimension(0, 50)); // 控制高度

        // 添加鼠标事件监听器以实现拖动功能
        searchField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint(); // 记录初始点击位置
            }
        });

        searchField.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (initialClick != null) {
                    // 获取当前窗口位置
                    int thisX = searchDialog.getLocation().x;
                    int thisY = searchDialog.getLocation().y;

                    // 计算拖动后的偏移量
                    int xMoved = e.getX() - initialClick.x;
                    int yMoved = e.getY() - initialClick.y;

                    // 设置新的窗口位置
                    searchDialog.setLocation(thisX + xMoved, thisY + yMoved);
                }
            }
        });


        int x = (screenSize.width - searchDialog.getWidth()) / 2;
        int y = (int) (screenSize.height * 0.2); // 距离顶部30%
        searchDialog.setLocation(x, y);
        contentPanel.add(searchField, BorderLayout.NORTH);

        // 搜索结果列表
        listModel = new DefaultListModel<>();
        resultList = new JBList<>(listModel);
        resultList.setFont(labelFont);
        resultList.setBackground(JBColor.background()); // 列表背景颜色
        resultList.setForeground(JBColor.foreground()); // 列表字体颜色
        resultList.setSelectionBackground(UIUtil.getListSelectionBackground(true)); // 选中背景
        resultList.setSelectionForeground(UIUtil.getListSelectionForeground(true)); // 选中字体颜色
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setForeground(JBColor.foreground()); // 强制字体颜色

                if (value != null && value.toString().endsWith("-已添加")) {
                    label.setForeground(JBColor.GREEN); // 已添加时字体颜色为绿色
                }

                label.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20)); // 设置内边距
                return label;
            }
        });

        // 滚动面板包装结果列表
        JScrollPane scrollPane = new JBScrollPane(resultList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVisible(false);// 初始时隐藏列表


        // 搜索框键盘事件监听
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String query = searchField.getText().trim();
                listModel.clear();
                if (!query.isEmpty()) {
                    List<String> results = handler.search(query);
                    if (!results.isEmpty()) {
                        updateSearchResults(results);
                        if (!scrollPane.isVisible()) {
                            contentPanel.add(scrollPane, BorderLayout.CENTER); // 动态添加滚动面板
                            scrollPane.setVisible(true);
                            searchDialog.setSize(600, 600); // 动态调整窗口大小
                            searchDialog.revalidate(); // 刷新布局
                        }
                    } else if (scrollPane.isVisible()) {
                        contentPanel.remove(scrollPane); // 动态移除滚动面板
                        scrollPane.setVisible(false);
                        searchDialog.setSize(600, 50); // 恢复窗口为仅显示输入框的大小
                        searchDialog.revalidate(); // 刷新布局
                    }
                } else {
                    if (scrollPane.isVisible()) {
                        contentPanel.remove(scrollPane);
                        scrollPane.setVisible(false);
                        searchDialog.setSize(600, 50);
                        searchDialog.revalidate();
                    }
                }

                // 支持上下键导航
                if (e.getKeyCode() == KeyEvent.VK_DOWN && !listModel.isEmpty()) {
                    resultList.requestFocus();
                    resultList.setSelectedIndex(0);
                }
            }
        });

        // 鼠标选择建议
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //双击选中实现字体变色并添加"已添加"字样 , 最后将选中值添加到表格中
                if (e.getClickCount() == 2 && resultList.getSelectedValue() != null) {
                    handleSelection();
                }
            }
        });

        // 结果列表键盘事件监听
        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && resultList.getSelectedValue() != null) {
                    handleSelection();
                }
            }
        });


        // 监听焦点丢失，关闭弹窗
        searchDialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // 窗口获得焦点时不做操作
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                searchDialog.setVisible(false); // 窗口失去焦点时关闭
            }
        });

        // 监听 Esc 键关闭对话框
        searchDialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        searchDialog.getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchDialog.setVisible(false); // 关闭窗口
            }
        });


    }

    //处理选中和取消选中数据追加到表格中
    private void handleSelection() {
        int selectedIndex = resultList.getSelectedIndex();
        String selectedValue = listModel.getElementAt(selectedIndex).toString();


        String key = getKeyForResult(selectedValue);

        if (selectedValue.endsWith("-已添加")) {
            // 移除 "-已添加" 并从 PropertiesComponent 中移除数据
            String originalValue = selectedValue.substring(0, selectedValue.length() - 4);
            listModel.setElementAt(originalValue, selectedIndex);
            resultList.repaint();

            // 从 PropertiesComponent 中移除数据
            String instanceCode = instance.getValue(key);
            if (instanceCode != null) {
                String[] split = originalValue.split("-");
                if (split.length == 3) {
                    String[] instanceSplit = instanceCode.split(";");
                    StringBuilder codeString = new StringBuilder();
                    for (String splitCode : instanceSplit) {
                        if (!splitCode.contains(split[1])) { // 关键：只添加不包含目标字符串且不为空的项
                            codeString.append(splitCode).append(";");
                        }
                    }
                    instance.setValue(key, codeString.toString());
                }
            }
        } else {
            // 添加 "-已添加" 并将数据添加到 PropertiesComponent
            listModel.setElementAt(selectedValue + "-已添加", selectedIndex);
            resultList.repaint();

            String[] split = selectedValue.split("-");
            if (split.length == 3) {
                String valueToAdd = split[1] + ";";
                String storedValue = instance.getValue(key);
                if (storedValue == null) {
                    storedValue = "";
                }
                storedValue += valueToAdd;
                instance.setValue(key, storedValue);
            }
        }
        apply();
    }

    private String getKeyForResult(String result) {
        if (result.startsWith("股票")) {
            return "key_stocks";
        } else if (result.startsWith("基金")) {
            return "key_funds";
        } else if (result.startsWith("债券")) {
            return "key_coins";
        }
        return ""; // 如果前缀不匹配，则返回 null
    }


    private static String getKeyForName(String tableName) {
        if (tableName.equals("Stock")) {
            return "key_stocks";
        } else if (tableName.startsWith("Fund")) {
            return "key_funds";
        } else if (tableName.startsWith("Coin")) {
            return "key_coins";
        }
        return ""; // 如果前缀不匹配，则返回 null
    }

    // 更新搜索结果 , 如果是历史已添加的数据 , 后面标记为已添加
    private void updateSearchResults(List<String> results) {
        listModel.clear();
        PropertiesComponent instance = PropertiesComponent.getInstance();
        for (String result : results) {
            String key = getKeyForResult(result);
            String storedValue = instance.getValue(key);
            if (storedValue != null) {
                if (result.split("-").length > 1 && storedValue.contains(result.split("-")[1])) {
                    result += "-已添加"; // 如果存在，则添加后缀
                }
            }
            listModel.addElement(result);
        }
    }

    // 全局快捷键监听
    private void bindGlobalKeyListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            boolean closeLog = PropertiesComponent.getInstance().getBoolean("key_close_f7");
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F7 && !closeLog) {
                listModel.clear(); // 清空结果
                if (!searchDialog.isVisible()) {
                    searchField.setText(""); // 清空输入框
                    searchDialog.setVisible(true); // 显示弹窗
                    searchField.requestFocus(); // 聚焦到输入框
                }
                return true; // 消费事件
            }
            //如果按了delete键 , 删除当前行
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_DELETE) {
                String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), handler.codeColumnIndex));//FIX 移动列导致
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    String key = getKeyForName(NAME);
                    // 从 PropertiesComponent 中移除数据
                    String storedValue = instance.getValue(key);
                    if (StringUtils.isNotBlank(storedValue)) {
                        String[] split = storedValue.split(";");
                        StringBuilder codeString = new StringBuilder();
                        for (String splitCode : split) {
                            if (!splitCode.contains(code) && !splitCode.isEmpty()) { // 关键：只添加不包含目标字符串且不为空的项
                                codeString.append(splitCode).append(";");
                            }
                        }
                        instance.setValue(key, codeString.toString());
                    }
                    apply();
                }
            }
            return false;
        });
    }


    private static StockRefreshHandler factoryHandler() {
        boolean useSinaApi = PropertiesComponent.getInstance().getBoolean("key_stocks_sina");
        if (useSinaApi) {
            if (handler instanceof SinaStockHandler) {
                return handler;
            }
            return new SinaStockHandler(table, refreshTimeLabel);
        }
        if (handler instanceof TencentStockHandler) {
            return handler;
        }
        return new TencentStockHandler(table, refreshTimeLabel);
    }

    public static void apply() {
        if (handler != null) {
            handler = factoryHandler();
            PropertiesComponent instance = PropertiesComponent.getInstance();
            handler.setStriped(instance.getBoolean("key_table_striped"));
            handler.clearRow();
            handler.setupTable(loadStocks());
            refresh();
        }
    }

    public static void refresh() {
        if (handler != null) {
            PropertiesComponent instance = PropertiesComponent.getInstance();
            handler.refreshColorful(instance.getBoolean("key_colorful"));
            List<String> codes = loadStocks();
            if (CollectionUtils.isEmpty(codes)) {
                stop(); //如果没有数据则不需要启动时钟任务浪费资源
            } else {
                handler.handle(codes);
                QuartzManager quartzManager = QuartzManager.getInstance(NAME);
                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put(HandlerJob.KEY_HANDLER, handler);
                dataMap.put(HandlerJob.KEY_CODES, codes);
                String cronExpression = instance.getValue("key_cron_expression_stock");
                if (StringUtils.isEmpty(cronExpression)) {
                    cronExpression = "*/10 * * * * ?";
                }
                quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
            }
        }
    }

    public static void stop() {
        QuartzManager.getInstance(NAME).stopJob();
        if (handler != null) {
            handler.stopHandle();
        }
    }

    private static List<String> loadStocks() {
//        return FundWindow.getConfigList("key_stocks", "[,，]");
        return SettingsWindow.getConfigList("key_stocks");
    }

}
