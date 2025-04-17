# leeks

![GitHub release (latest by date)](https://img.shields.io/github/v/release/RainyQing/leeks?label=RELEASE&style=flat-square&logo=github&color=green)
![star](https://img.shields.io/github/stars/RainyQing/leeks?style=flat-square&logo=github)

idea插件，查看基金，股票：支持A股，港股，美股

## 申明

### Leeks (Fork)

本项目是 [leeks](https://github.com/huage2580/leeks) 的一个 fork，由 [huage2580](https://github.com/huage2580)
开发。原项目似乎已停止维护，因此我（[RainyQing](https://github.com/RainyQing)）创建了这个 fork 以继续维护和改进。

**重要提示：原项目似乎没有提供明确的开源许可证。因此，本 fork 仅供个人学习、研究和使用，不得用于任何商业用途或进行分发。**

### 维护状态

本项目目前正积极维护中，我会定期修复 bug 并添加新功能。

### 贡献

欢迎提交 issue 和 bug 报告。目前接受针对 bug 修复和小型改进的 pull request。请确保您的 pull request 符合现有的代码风格并包含适当的测试。

### 免责声明

本项目按“原样”提供，不提供任何形式的明示或暗示的保证，包括但不限于适销性、特定用途的适用性和非侵权性保证。项目使用的所有代码以及请求接口仅用于学习、研究目的。在任何情况下，作者或版权所有者均不对因使用本项目而引起的任何索赔、损害或其他责任承担责任，无论是在合同诉讼、侵权诉讼或其他方面。

## 请先阅读完readme，确保编码正确输入

提issues附上:使用的插件版本、IDEA详细的版本信息(到Help->about里面复制出来)，如果【Event Log】有异常信息，也请在issues附上异常

## 安装

从 [插件下载地址](https://github.com/RainyQing/leeks/releases)   下载最新的 leeks-x.x.x.zip 文件。
在 IntelliJ IDEA 中，转到 File > Settings > Plugins > Install Plugin from Disk...。
选择下载的 leeks-x.x.x.zip 文件。不要解压 ZIP 文件。
重启 IntelliJ IDEA。

## 特性

* 支持 A 股、港股、美股和加密货币行情。
* 通过 F7 快捷键快速添加/删除股票和基金。
* 支持成本价、持有份额、收益和收益率估算。
* 提供分时图和 K 线图（股票）。
* 可自定义更新时间间隔（使用 Cron 表达式）。
* 提供隐蔽模式（拼音显示和无颜色涨跌幅）。
* 可配置代理服务器。

## 使用

### 一、配置

#### 快捷配置(强烈推荐)
* 在idea界面按F7弹出输入框 , 输入股票代码/名字/名字缩写搜索股票 , 双击/上下键选择并按回车进行股票新增或者删除。在股票界面选中股票鼠标右键也可以进行删除操作。
![da](img/img3.png)
![da](img/img4.png)

* 可以直接在股票界面配置成本价和持仓 , 不用通过设置界面配置 , 简化操作 (目前仅支持股票) (强烈推荐)

  <span style="color:red; font-weight:bold;">注意 : 仅编辑成本价不会显示成本价和收益 , 收益率 , 需配合编辑持仓才会显示 , 仅填写持仓不会出现这种情况</span>
![da](img/img5.png)




#### 使用设置界面中配置

设置里面找到Leeks选项，输入基金编码，股票编码，英文分号分割【;】，apply。    
可以输入成本价和持有份额，估算收益和收益率，编码后通过英文逗号【,】拼接成本价和份额即可。

`隐蔽模式默认开启，开启无着色，并且拼音显示，可以自行关闭。`

基金示例：`006250,3.66,1000;110013;`  
股票示例：`sh000001;sh000300;sh600111,55.7,500;hk00700;usAAPL;usBILI`  
加密货币示例：`BTC-USD,DOGE-USD`

**股票编码有前缀**，示例代码：（sh000001;sh600519;sz000001;hk00700;usAAPL）`股票编码前缀小写`，建议用雪球看网页找

基金编码zfb上面有，或者天天基金看

double shift，连按两下shift，输入leeks，找到toolWindow，打开以后默认在下方，自行调节位置  
每次修改，添加基金,股票，只需点击apply自动生效。    
基金更新频率一分钟一次，股票10s一次

### 二、加密货币的选项

加密货币行情采用雅虎的接口，必须使用代理才能获取数据，编码格式如下:  
例:`比特币`货币的编码为`BTC-USD`,【加密货币代码-USD】

### 三、分时图和K线图

基金只有估值分时图，股票提供分时图，日周月K线图，非图表，不能进行操作，不会自动刷新。右键弹出菜单选择K线图。

### 四、代理

插件不会使用系统或者IDEA的代理，请前往插件设置页设置代理。格式为`127.0.0.1:1080` 这样子。K线图那些不会走代理

### 五、设置更新的时间区间

使用Cron表达式设置更新区间，在线cron工具 https://www.bejson.com/othertools/cron/  
关于设置Cron表达式组合示例：
【\*/10 30-59 9 ? * 2-6;\*/10 0-30 11 ? * 2-6;\*/10 * 10,13,14 ? * 2-6】表示周一至周五，9:30-11:30 ，13:00-15:00，每隔10秒刷新一次；
多个表达式不要出现时间交集，因为交集内会增加运行次数。

## 预览

![da](img/img1.png)
![dd](img/img2.png)
![da](img/img3.png)
![da](img/img4.png)
![da](img/img5.png)
## 常见问题

* 异常日志中出现【java.lang.NoClassDefFoundError: com/github/promeg/pinyinhelper/Pinyin】  
  解决：请直接选择【编译后的zip文件】进行安装，不要解压zip文件。
* 股票数据内容显示都是符号【-】  
  解决：确保填写的股票数据格式不正确，有小写前缀。示例（sh000001;sh600519;sz000001;hk00700;usAAPL）

## change

- v1.1
    * 增加了股票的tab，采用腾讯的行情接口，股票轮询间隔10s
- v1.2
    * 支持了港股和美股 示例代码：（sh000001,sh600519,sz000001,hk00700,usAAPL）代码一般可以在各网页端看得到
- v1.3
    * 插件由小韭菜更名为Leeks
    * 支持了IDEA 2020.1.3,兼容到`IDEA 2017.3`，修复macOS 行高问题
- v1.4.1
    * 增加了隐蔽模式（全拼音和无色涨跌幅
- v1.4.2
    * 支持到IDEA 2020.2.*
- v1.5.1
    * 增加了股票界面按表头排序，设置界面及时生效，不用点击refresh按钮啦 merge
      from [dengerYang](https://github.com/dengerYang)
- v1.5.2
    * 增加了股票的最高价最低价 . merge from [dengerYang](https://github.com/dengerYang)
- v1.6.1
    * 样式和bug fix,(样式调整，增加当日净值merge from [dengerYang](https://github.com/dengerYang) )
- v1.6.2
    * 适配IDEA 2020.3
- v1.6.3
    * 修复颜色错乱问题 , 日志调整 merge from [qwn3213](https://github.com/qwn3213)
- v1.7.1
    * 增加日志开关 ,设置界面样式调整 merge from [dengerYang](https://github.com/dengerYang) ，增加新浪股票接口备选 merge
      from [JulianXG](https://github.com/JulianXG)
- v1.8.1
    * 增加了虚拟货币的界面
- v1.8.3
    * 增加了分时图和K线图 merge from [dengerYang](https://github.com/dengerYang)
- V1.8.4
    * bug fix from [DAIE](https://github.com/DA1Y1)
- V1.8.5
    * 保存表头顺序 from [DAIE](https://github.com/DA1Y1)
- V1.9.1
    * 图表界面优化 from [dengerYang](https://github.com/dengerYang)
- V1.9.3
    * 加入代理设置
- V1.9.5
    * 虚拟币行情接口切换为雅虎，必须使用代理才能获取数据
- V1.9.8
    * 支持基金和股票成本价，持仓，收益率，收益显示 from [chenheng](https://github.com/RoaringFlame)
- V1.9.9
    * 修复成本价过低时收益金额不正确的问题；from [神驱一梦](https://github.com/BorrisWQBi)
- v2.0.1
    * 支持自定义更新的时间段 from [dengerYang](https://github.com/dengerYang)
- v2.1.0
    * 兼容成本为负的情况 from [bu6030](https://github.com/bu6030)
- v2.2.0
    * 适配IDEA 2023.3 from [WoChen5770](https://github.com/WoChen5770)
- v2.3.0
    * 修复腾讯接口港股实时数据。（此前是延时15分钟）
    * 更新新浪接口支持港美股。 from [WoChen5770](https://github.com/WoChen5770)
- v2.4.0
    * 适配IDEA 2024.1 from [WoChen5770](https://github.com/WoChen5770)
- v2.5.0
  适配IDEA 2024.3 from [RainyQing](https://github.com/RainyQing)
- v2.6.0
    * 优化代码 , 使用注解替换所有getter/setter方法
    * 新增卖一/买一手数表头显示
    * 优化程序开始初始化表头逻辑 , 新增如果添加/删除固定表头同时修改当前表头
      from [RainyQing](https://github.com/RainyQing)
- v2.7.0
    * 新增按F7刷新所有股票/基金数据并进行添加/移除操作
    * 股票界面鼠标右键新增删除按钮 from [RainyQing](https://github.com/RainyQing)
- v2.8.0
    * 新增点击股票代码行对应得持仓/成本价cell进入编辑模式 from [RainyQing](https://github.com/RainyQing)
- v2.9.0
  * 修复置空成本价和持仓数据不正确bug
  * 新增单行选中股票使用快捷键delete键删除 from [RainyQing](https://github.com/RainyQing)
- v2.9.1
  * 修复插件兼容IDEA 2025.1 from [RainyQing](https://github.com/RainyQing)

