package bean;

import lombok.Data;

import java.text.DecimalFormat;

@Data
public class CoinBean {
    static DecimalFormat decimalFormat = new DecimalFormat("#.00");

    private String symbol;
    private double regularMarketPrice;
    private double regularMarketDayHigh;
    private double regularMarketDayLow;
    private double regularMarketChange;
    private double regularMarketChangePercent;
    private long regularMarketTime;


    public CoinBean(String code) {
        this.symbol = code;
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
                return this.getSymbol();
            case "涨跌":
                return String.valueOf(this.getRegularMarketChange());
            case "涨跌幅":
                return decimalFormat.format(this.getRegularMarketChangePercent()) + "%";
            case "最高价":
                return String.valueOf(this.getRegularMarketDayHigh());
            case "最低价":
                return String.valueOf(this.getRegularMarketDayLow());
            case "当前价":
                return String.valueOf(this.getRegularMarketPrice());
            case "更新时间":
                String timeStr = "--";
                if (this.getRegularMarketTime() > 0) {
                    timeStr = String.valueOf(this.getRegularMarketTime());
                }
                return timeStr;

            default:
                return "";

        }
    }
}
