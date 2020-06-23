## schedule-framework
Orion schedule 是一个高性能的分布式任务框架，通过任务调度和任务执行的分离，从而可以小规模的调度集群调度大规模的执行队列，同时保证调度请求不会因个别任务执行耗时影响其他服任务
该框架的主要特性如下
- 任务调度和任务执行分离，保证调度的效率和精准度
两台调度集群即可调度成千上万个任务实例
- 虚拟分组
突破应用限制，分组内的机器可以执行相同的任务
- 水平扩容无感知 
任务执行实例可无限水平扩展，通过服务发现自动感知集群数据量变更
- 进度感知
执行进度按批次上报到任务监控服务中
- 多级分发，保证负载均衡
保证任务执行过程各机器负载基本均衡，不会出现因数据倾斜造成负载不均
- 幂等性
任意一个任务分发批次保证只会最多派发成功一次，失败三次则放弃
- 多数据源支持
待执行的任务数据包括但不限于文件/数据库/消息队列/缓存等

具体配置可参考[配置列表](#配置列表)

## 整体框架
![schedule_architure](https://user-images.githubusercontent.com/66338301/85259237-480d8e80-b49b-11ea-8fa3-91749f9a9301.png)
## 代码组成
整个框架分三个核心部分和一个demo部分
- schedule-core 整个框架的核心实现
- schedule-console 任务的调度服务，同时承担任务管理的一部分职能
- console-web 管理控制台，前端页面，vue实现
- schedule-demo 调度例子

## How to use it 
### 编译安装
- schedule-core 
```bash
git clone https://github.com/orion-open-group/schedule-core
cd schedule-core
mvn install -Dmaven.test.skip=true
```
- schedule-console

任务调度服务，下载后需要同步创建数据库,建库脚本在resource中
```bash
git clone https://github.com/orion-open-group/schedule-console
cd schedule-console
mvn install -Dmaven.test.skip=true
java -jar schedule-starter/target/schedule-starter.jar
```
- console-web

提供管理功能，下载后可直接用npm打包运行
```bash
git clone https://github.com/orion-open-group/console-web
cd console-web
cnpm install
cnpm run dev
``` 
默认登录用户是admin/123123

### 配置任务
#### 分组配置
整个任务调度是基于虚拟分组进行调度，首先需要新建一个虚拟分组，用于标记哪些机器归属这一个分组，这个操作在分组管理里完成
#### 任务开发
maven项目中需要新增 schedule-client-starter 依赖，版本用schedule-core里的版本即可
```xml
 <dependency>
    <groupId>com.orion.schedule</groupId>
    <artifactId>schedule-client-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

按照实际的需求，任务执行可分为如下三类
- 单机任务
其中单机任务就是调度只发生在一台机器上，可通过继承StandaloneJobProcessor来实现
```text
com.orion.schedule.processor.standalone.StandaloneJobProcessor
```
- 分布式任务
分布式任务则是任务在虚拟分组内的机器上并行执行，需继承DistributedJobProcessor来实现
```text
com.orion.schedule.processor.distribute.DistributedJobProcessor
```
- 分布式多阶段任务
网格计算则支持多阶分发，多次派发主要解决数据不均衡造成待执行任务倾斜在一台机器实例上造成任务执行耗时长，机器负载不均衡的问题
```text
com.orion.schedule.processor.grid.GridJobProcessor
```
每一个定时任务需要解决两个问题，从哪里取数据，如何执行，任务调度框架则将这两部分进行抽象
从任意可能的数据源取数后直接 分批次dispatch，集群中的其他机器接受到待执行数据后则直接多线程并行执行
如需二次分发请在每个阶段执行完毕后单独dispatch, 所有的实现都需要实现取数和执行两个函数，即
- fetchData
- processData
实现完毕后即可在任务配置平台进行配置,调度

#### 任务配置
任务配置则将基于分组配置任务的基本信息，相对比较简单，上下文中可以填写部分信息，这部分信息在任务执行过程中可以带过去

## <a id="#配置列表">配置列表</a>
```yaml
schedule:
    server:
      codec:
        code: jackson-msg-pack  ##调度指令及任务分发过程中报文的压缩编码格式，默认jsonpack
      transport:
        code: netty4 ## 指令下发及报文的传输通道
        config:
          serverPort: xxx, ## 客户端引入时需要，接受指令及任务数据的端口，不同的虚拟分组需保持不同
          idleTime:  xxx ## 保持心跳的时间间隔，默认5秒
          connectionTimeOut: xxx ## 连接超时时间，默认5秒
          closeWait: xx ## 优雅关闭时，连接关闭时间
      register:
        code: xxx ##注册中心类型，目前支持 nacos,etcd，后续可扩展其他类型
        config:  ## nacos配置信息
          namespace: xxx ## nacos命名空间
          serverList: ##nacos服务端信息
            - 10.10.10.10
            - 10.20.20.20 
        etcdConfig: ## etcd配置
          serverList: ## serverList信息
            - 10.10.10.10
            - 10.20.20.20 
          ttl: xx ## 心跳时间
      task: 
        groupList:  ## 虚拟分组信息，多个虚拟分组可以在一个任务配置中 
          - xx
          - xx
        processThread: xx, ##任务执行端的并发度，同一个应用的多个虚拟分组共享此并发度
        noticeType: xx ## 任务进度上报方式，目前支持'KAFKA','DB'
        dataSource: ## noticeType为DB时需要配置此参数集，用于指明如何上报任务执行进度信息
          url: xxx  ## 连接url，需要用com.orion.schedule.progress.util.ScheduleEncrypt加密
          userName: xx ## 用户名称 同上需要加密
          password:  xx ## 用户密码 同上需要加密
          token: xxx ## 和上述三个二选1，将三个维度信息放到一个json中再统一用上述方式加密
        messageConfig: ## 当noticeType为KAFKA时需要补充此信息
          register: xxx ## kafka的服务端地址
          useSSL: xx ## 是否用ssl链接 默认false
          topic: xxx ## kafka的topic信息
          useKerberos: xxx ## 是否用kerberos认证，默认false
          krbPath: xxxx ## krb配置文件路径
          krbTabPath: xxx ## keyTab文件路径
          kerberosUser: xxx ## kerberors 用户

```

### 配置的分类
### 调度端
- codec
- transport
- register
服务端的配置可参考 [schedule-console](https://github.com/orion-open-group/schedule-console)
### 任务接入端
- codec
- transport
- register
- task
接入端的配置可参考 [schedule-demo](https://github.com/orion-open-group/schedule-demo)
