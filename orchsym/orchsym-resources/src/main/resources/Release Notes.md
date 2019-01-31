

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


