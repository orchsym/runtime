
----------------------------
# Version 2.0_07000

版本：1.7.1-2.0_07000

发布时间：2019-07-23

组件总数：312

## 新功能
- [ORCHSYM-4165] - [组件]开发SortRecord数据流排序组件
- [ORCHSYM-4168] - [组件]开发FilterField字段过滤组件
- [ORCHSYM-4704] - [组件]增加Excel文件读取组件
- [ORCHSYM-4812] - [组件]开发支持LDAP组件
- [ORCHSYM-4188] - 支持flow版本管理
- [ORCHSYM-4273] - 适配管理平台支持多集成平台管理

## 改进
- [ORCHSYM-4265] - PutTCP、PutUDP、PutSplunk 参数支持属性变量
- [ORCHSYM-4739] - 增强提取属性组件功能
- [ORCHSYM-4818] - InvokeHTTP的DELETE不支持接收Body
- [ORCHSYM-4936] - 编排支持ADFS 3.0登录

## 缺陷 
- [ORCHSYM-4661] - 文档目录遍历问题
- [ORCHSYM-4688] - 升级Groovy解决编码问题
- [ORCHSYM-4785] - 编排平台日志轮滚时,maxHistory 和 totalSizeCap失效
- [ORCHSYM-4816] - Log4j安全漏洞修复
- [ORCHSYM-4835] - repository库更新或读取出错
- [ORCHSYM-4852] - API服务后台swagger请求失败
- [ORCHSYM-4878] - SAP RFC有table输出时报错
- [ORCHSYM-4912] - EL表达式在属性设置中提示值带序号

----------------------------
# Version 2.0_06001

版本：1.7.1-2.0_06001

发布时间：2019-07-09

组件总数：303


## 缺陷 
- [ORCHSYM-4852] - API服务后台swagger请求失败


----------------------------
# Version 2.0_06000

版本：1.7.1-2.0_06000

发布时间：2019-06-19

组件总数：303

## 新功能
- [ORCHSYM-4078] - 集成平台前端改版
- [ORCHSYM-4157] - 支持集群模式下登录token共享
- [ORCHSYM-4273] - 适配管理平台支持多集成平台管理

## 改进
- [ORCHSYM-4365] - 库日志支持详细错误堆栈
- [ORCHSYM-4408] - 修改PutDatabaseRecord字段转换默认值
- [ORCHSYM-4670] - 增强SignatureProcessor组件支持大文件
- [ORCHSYM-4391] - 完成全部组件或控制器翻译

## 缺陷 
- [ORCHSYM-4412] - 组件分类不正确
- [ORCHSYM-4108] - 数据朔源功能失效
- [ORCHSYM-4375] - 单一nar包无法加载导致无法启动
- [ORCHSYM-4390] - HandleHttpRequest 请求超时
- [ORCHSYM-4407] - HandlerHttpRequest 基本认证校验异常
- [ORCHSYM-4447] - flowfile更新失败
- [ORCHSYM-4638] - HandleSOAPHTTPRequest组件启动失败

----------------------------
# Version 2.0_05000

版本：1.7.1-2.0_05000

发布时间：2019-05-17

组件总数：303

## 新功能
- [ORCHSYM-2898] - 模块底层数据增加分类元数据
- [ORCHSYM-3066] - OPC 标准支持DA
- [ORCHSYM-3731] - PutFile 缺少Append 文件内容选项
- [ORCHSYM-4183] - 黑名单组件支持预览功能

## 改进
- [ORCHSYM-3515] - 前端支持表达式提示信息
- [ORCHSYM-4041] - 启用组件黑名单功能
- [ORCHSYM-4125] - 数据库组件日志完善
- [ORCHSYM-4176] - GetFile组件修改默认为不删除源文件
- [ORCHSYM-4100] - 校正表达式语言手册
- [ORCHSYM-3707] - 翻译第四阶段

## 缺陷 
- [ORCHSYM-2887] - Runtime自动刷新请求丢失Token



----------------------------
# Version 2.4.0

版本：1.7.1-2.4.0

发布时间：2019-04-18

组件总数：302

## 新功能
- [ORCHSYM-3652] - 创建支持Redis存储失效时间的组件
- [ORCHSYM-3201] - 创建Orchsym web api

## 改进
- [ORCHSYM-3492] - 优化SAP RFC数据输出

## 缺陷 
- [ORCHSYM-2918] - 编排平台启用权限认证后flow根权限为只读
- [ORCHSYM-3531] - 组件国际化不支持带版本的组件
- [ORCHSYM-3601] - 存储过程ExecuteStoreProcedure组件问题
- [ORCHSYM-3644] - HandleHTTPRequest开启API注册功能启动后Method不正确
- [ORCHSYM-3679] - 控制器服务应默认启动
- [ORCHSYM-3725] - XML和JSON转换组件丢失属性
- [ORCHSYM-3824] - 编排平台用户列表显示不了管理平台创建并登录的账号
- [ORCHSYM-3950] - 认证用户登录集成平台后默认不创建账号

----------------------------
# Version 2.3.1

版本：1.7.1-2.3.1

发布时间：2019-03-12

组件总数：301

## 新功能
- [ORCHSYM-2984] - 增加表达式语言日期操作函数

## 改进
- [ORCHSYM-2843] - 将集成平台前端JSP转换为HTML
- [ORCHSYM-3183] - 组件翻译第二阶段

## 缺陷 
- [ORCHSYM-3358] - 组件UpdateAttribute无法在高级设置中添加规则

----------------------------
# Version 2.3.0

版本：1.7.1-2.3.0

发布时间：2019-02-28

组件总数：301

## 新功能
- [ORCHSYM-3113] - 登录对接ADFS
- [ORCHSYM-3205] - 分离集群心跳日志到orchsym-cluster.log
- [ORCHSYM-3184] - 定制开发Dubbox master支持组件

## 改进
- [ORCHSYM-2843] - 将集成平台前端JSP转换为HTML
- [ORCHSYM-2851] - PutDistributedMapCache增加支持动态属性设置
- [ORCHSYM-3028] - 增强InvokeSOAP组件
- [ORCHSYM-3065] - 验证是否支持Dubbo最新版2.6.5
- [ORCHSYM-3163] - 修改HandleHttpRequest默认端口
- [ORCHSYM-3053] - 统一编排和管理平台3.0的许可

## 缺陷
- [ORCHSYM-3126] - 组件ExtractAvroToAttributes加载失败
- [ORCHSYM-3164] - 删除组件不提示确认信息
- [ORCHSYM-3328] - 回滚ORCHSYM-2700升级Groovy版本

----------------------------
# Version 2.2.0

2019新春祝福版

版本：1.7.1-2.2.0

发布时间：2019-01-30

组件总数：300

## 新功能
 - [ORCHSYM-2473] - [Processor]提供简便的XML与JSON互转组件
 - [ORCHSYM-2558] - [Processor]提供简便的XML与JSON抽取成属性
 - [ORCHSYM-2845] - 升级集成平台到NiFi 1.7.1正式版
 - [ORCHSYM-2880] - 编排平台需要支持 OEM 配置
 - [ORCHSYM-2998] - 分离集成平台CE和EE代码库

## 改进
 - [ORCHSYM-2652] - 支持队列查看文件内容或格式化支持最大行数或record记录数
 - [ORCHSYM-2700] - 升级集成平台中组件Groovy版本
 - [ORCHSYM-2964] - HandleHttpRequest 组件将 allowed path 设置为必填项
 - [ORCHSYM-2973] - HandleHTTPRequest组件功能增强
 - [ORCHSYM-3036] - 提升编排平台数据存储配置默认值
 - [ORCHSYM-3064] - 支持Dubbox


## 缺陷
 - [ORCHSYM-2968] - host:8443/nifi-api/apis 接口报 500 错误
 - [ORCHSYM-3000] - 无法全部启动当前模块的所有组件
 - [ORCHSYM-3063] - 组件CRON 设置始终无效
 - [ORCHSYM-3098] - LogMessage 不能正常处理Flowfile

----------------------------
# Version 2.1.1

版本：1.7.0-2.1.1

发布时间：2019-01-15

组件总数：287

## 缺陷
 - [ORCHSYM-2968] - host:8443/nifi-api/apis 接口报 500 错误
 

----------------------------
# Version 2.1.0

圣诞特别版

版本：1.7.0-2.1.0

发布时间：2018-12-24

组件总数：287

## 新功能
 - ORCHSYM-2462/组件的国际化语言支持
 - ORCHSYM-2441/支持用户切换多语言
 - ORCHSYM-1533/删除组件时，提供二次确认功能
 - ORCHSYM-1699/EDI组件
 - ORCHSYM-2792/一键启动当前模块的ControllerService 
 - ORCHSYM-2530/管理平台2.0需支持编排平台同步的path参数类型的API
 - ORCHSYM-2782/集成平台支持管理额外依赖包
 - ORCHSYM-2603/对于没有输入的部分运行安排设置是0秒的组件，在运行前需提供一警告消息
        
## 改进
 - ORCHSYM-2150/当组件执行失败，多次尝试后日志刷爆系统
 - ORCHSYM-890/InvokeSOAP组件增强
 - ORCHSYM-2469/新用户登录时，创建新用户并设置对应默认权限
 - ORCHSYM-2439/缺省的管理员应当拥有所有的用户权限
 - ORCHSYM-2586/InvokeHTTP启用https(SSL)后，提供忽略认证选项
 - ORCHSYM-2663/组件打包时，需要支持可选打包system依赖
 - ORCHSYM-2759/修改about对话框，返回实际的版本信息
 - ORCHSYM-2756/“最大定时器驱动线程数”默认值过小，导致并发失败
 - ORCHSYM-2731/集成平台下载模板功能完善
 - ORCHSYM-2522/重构API Service，并直接作为runtime可配置内置功能
 - ORCHSYM-2888/菜单名称不显示

 ----------------------------
# Version tag 004

圣诞特别版

版本：1.7.0-004

发布时间：2018-10-25

组件总数：283


