package bean;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import utils.PinYinUtils;

import java.util.Map;

@Data
public class StockBean {
    private String code;
    private String name;
    private String now;
    private String change;//涨跌
    private String changePercent;
    private String time;
    private String buyOne;
    private String sellOne;
    /**
     * 最高价
     */
    private String max;
    /**
     * 最低价
     */
    private String min;

    private String costPrise;//成本价
    //    private String cost;//成本
    private String bonds;//持仓
    private String incomePercent;//收益率
    private String income;//收益

    //配置code同时配置成本价和成本值
    public StockBean(String code) {
        if (StringUtils.isNotBlank(code)) {
            String[] codeStr = code.split(",");
            if (codeStr.length > 2) {
                this.code = codeStr[0];
                this.costPrise = codeStr[1];
//                this.cost = codeStr[2];
                this.bonds = codeStr[2];
            } else {
                this.code = codeStr[0];
                this.costPrise = "--";
//                this.cost = "--";
                this.bonds = "--";
            }
        } else {
            this.code = code;
        }
        this.name = "--";
    }

    public StockBean(String code, Map<String, String[]> codeMap) {
        this.code = code;
        if (codeMap.containsKey(code)) {
            String[] codeStr = codeMap.get(code);
            if (codeStr.length > 2) {
                this.code = codeStr[0];
                this.costPrise = codeStr[1];
//                this.cost = codeStr[2];
                this.bonds = codeStr[2];
            }
        }
    }


    /**
     * 返回列名的VALUE 用作展示
     *
     * @param colums   字段名
     * @param colorful 隐蔽模式
     * @return 对应列名的VALUE值 无法匹配返回""
     */
    public String getValueByColumn(String colums, boolean colorful) {
        switch (colums) {
            case "编码":
                return this.getCode();
            case "股票名称":
                return colorful ? this.getName() : PinYinUtils.toPinYin(this.getName());
            case "当前价":
                return this.getNow();
            case "买一":
                return this.getBuyOne();
            case "卖一":
                return this.getSellOne();
            case "涨跌":
                String changeStr = "--";
                if (this.getChange() != null) {
                    changeStr = this.getChange().startsWith("-") ? this.getChange() : "+" + this.getChange();
                }
                return changeStr;
            case "涨跌幅":
                String changePercentStr = "--";
                if (this.getChangePercent() != null) {
                    changePercentStr = this.getChangePercent().startsWith("-") ? this.getChangePercent() : "+" + this.getChangePercent();
                }
                return changePercentStr + "%";
            case "最高价":
                return this.getMax();
            case "最低价":
                return this.getMin();
            case "成本价":
                return this.getCostPrise();
            case "持仓":
                return this.getBonds();
            case "收益率":
                return this.getCostPrise() != null ? this.getIncomePercent() + "%" : this.getIncomePercent();
            case "收益":
                return this.getIncome();
            case "更新时间":
                String timeStr = "--";
                if (this.getTime() != null) {
                    timeStr = this.getTime().substring(8);
                }
                return timeStr;
            default:
                return "";

        }
    }
}
