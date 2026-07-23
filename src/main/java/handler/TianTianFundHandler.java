package handler;

import bean.FundBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TianTianFundHandler extends FundRefreshHandler {
    public final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private JLabel refreshTimeLabel;

    public TianTianFundHandler(JTable table, JLabel refreshTimeLabel) {
        super(table);
        this.refreshTimeLabel = refreshTimeLabel;
    }

    @Override
    public void handle(List<String> code) {
        //LogUtil.info("Leeks 更新Fund编码数据.");

        if (code.isEmpty()) {
            return;
        }

        stepAction(code);
    }

    @Override
    public void stopHandle() {
        LogUtil.info("Leeks 准备停止更新Fund编码数据.");
    }

    private void stepAction(List<String> codes) {
//        LogUtil.info("Leeks 刷新基金数据.");
        List<String> codeList = new ArrayList<>();
        Map<String, String[]> codeMap = new HashMap<>();
        for (String str : codes) {
            //兼容原有设置
            String[] strArray;
            if (str.contains(",")) {
                strArray = str.split(",");
            } else {
                strArray = new String[]{str};
            }
            codeList.add(strArray[0]);
            codeMap.put(strArray[0], strArray);
        }

        for (String code : codeList) {
            new Thread(() -> {
                try {
                    String result = HttpClientPool.getHttpClient().get(
                            "https://hq.sinajs.cn/list=fu_" + code, "GB18030");
                    String prefix = "var hq_str_fu_" + code + "=\"";
                    int start = result.indexOf(prefix);
                    if (start < 0) {
                        LogUtil.info("Fund编码:[" + code + "]无法获取数据");
                        return;
                    }
                    int end = result.indexOf("\";", start + prefix.length());
                    if (end < 0) {
                        LogUtil.info("Fund编码:[" + code + "]无法获取数据");
                        return;
                    }
                    String dataStr = result.substring(start + prefix.length(), end);
                    String[] values = dataStr.split(",");
                    if (values.length < 8 || StringUtils.isAnyBlank(values[0], values[2], values[3], values[6], values[7])) {
                        LogUtil.info("Fund编码:[" + code + "]返回数据不完整");
                        return;
                    }
                    FundBean bean = new FundBean(code);
                    bean.setFundCode(code);
                    bean.setFundName(values[0]);
                    bean.setGsz(values[2]);
                    bean.setDwjz(values[3]);
                    bean.setGszzl(values[6]);
                    bean.setJzrq(values[7]);
                    bean.setGztime(values[7] + " " + values[1]);
                    FundBean.loadFund(bean, codeMap);

                    BigDecimal now = new BigDecimal(bean.getGsz());
                    String costPriceStr = bean.getCostPrise();
                    if (NumberUtils.isParsable(costPriceStr)) {
                        BigDecimal costPriceDec = new BigDecimal(costPriceStr);
                        BigDecimal incomeDiff = now.add(costPriceDec.negate());
                        if (costPriceDec.compareTo(BigDecimal.ZERO) <= 0) {
                            bean.setIncomePercent("0");
                        } else {
                            BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 8, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.TEN)
                                    .multiply(BigDecimal.TEN)
                                    .setScale(3, RoundingMode.HALF_UP);
                            bean.setIncomePercent(incomePercentDec.toString());
                        }

                        String bondStr = bean.getBonds();
                        if (NumberUtils.isParsable(bondStr)) {
                            BigDecimal bondDec = new BigDecimal(bondStr);
                            BigDecimal incomeDec = incomeDiff.multiply(bondDec)
                                    .setScale(2, RoundingMode.HALF_UP);
                            bean.setIncome(incomeDec.toString());
                        }
                    }

                    SwingUtilities.invokeLater(() -> {
                        updateData(bean);
                        updateUI();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void updateUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refreshTimeLabel.setText(LocalDateTime.now().format(timeFormatter));
                refreshTimeLabel.setToolTipText("最后刷新时间");
            }
        });
    }

}
