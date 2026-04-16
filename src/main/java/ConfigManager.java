import com.intellij.ide.util.PropertiesComponent;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();
    private final PropertiesComponent properties;
    
    private ConfigManager() {
        this.properties = PropertiesComponent.getInstance();
    }
    
    public static ConfigManager getInstance() {
        return INSTANCE;
    }
    
    // 获取基金代码列表
    public List<String> getFundCodes() {
        return getConfigList(ConfigKeys.KEY_FUNDS);
    }
    
    // 获取股票代码列表
    public List<String> getStockCodes() {
        return getConfigList(ConfigKeys.KEY_STOCKS);
    }
    
    // 获取加密货币代码列表
    public List<String> getCoinCodes() {
        return getConfigList(ConfigKeys.KEY_COINS);
    }
    
    // 获取表格条纹显示设置
    public boolean isTableStriped() {
        return properties.getBoolean(ConfigKeys.KEY_TABLE_STRIPED, false);
    }
    
    // 获取彩色显示设置
    public boolean isColorfulEnabled() {
        // Keep legacy behavior: default to super-hide mode when unset.
        return properties.getBoolean(ConfigKeys.KEY_COLORFUL, false);
    }
    
    // 获取代理设置
    public String getProxySetting() {
        return properties.getValue(ConfigKeys.KEY_PROXY, "");
    }
    
    // 获取基金 Cron 表达式
    public String getFundCronExpression() {
        return properties.getValue(ConfigKeys.KEY_CRON_EXPRESSION_FUND, "0 * * * * ?");
    }
    
    // 获取股票 Cron 表达式
    public String getStockCronExpression() {
        return properties.getValue(ConfigKeys.KEY_CRON_EXPRESSION_STOCK, "*/10 * * * * ?");
    }
    
    // 获取加密货币 Cron 表达式
    public String getCoinCronExpression() {
        return properties.getValue(ConfigKeys.KEY_CRON_EXPRESSION_COIN, "*/10 * * * * ?");
    }
    
    // 获取股票接口设置
    public boolean useSinaStockApi() {
        return properties.getBoolean(ConfigKeys.KEY_STOCKS_SINA, false);
    }
    
    // 获取 F7 快捷键设置
    public boolean isF7Enabled() {
        return !properties.getBoolean(ConfigKeys.KEY_CLOSE_F7, false);
    }
    
    // 通用配置列表加载方法
    public List<String> getConfigList(String key) {
        return getConfigList(key, "[;]");
    }
    
    // 带分隔符的配置列表加载方法
    public List<String> getConfigList(String key, String separator) {
        List<String> list = new ArrayList<>();
        String value = properties.getValue(key);
        if (StringUtils.isNotBlank(value)) {
            String[] split = value.split(separator);
            for (String s : split) {
                if (StringUtils.isNotBlank(s)) {
                    list.add(s.trim());
                }
            }
        }
        return list;
    }
    
    // 保存配置列表
    public void saveConfigList(String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            properties.setValue(key, "");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(value).append("; ");
        }
        properties.setValue(key, sb.toString().trim());
    }
    
    // 保存单个配置
    public void saveValue(String key, String value) {
        properties.setValue(key, value);
    }
    
    // 保存布尔配置
    public void saveBoolean(String key, boolean value) {
        properties.setValue(key, String.valueOf(value));
    }
}
