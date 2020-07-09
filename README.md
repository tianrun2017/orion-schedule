Welcome to Use this Distributed Task Framework

This framework support nacos/etcd/zookeeper/eureka/db as it's service discover now,if you neeed another kind of service discover service ,
please notice me ,i will add the feature as soon as possible, You can add the feature by yourself ,it's very simple
please read the register module ,there is only a few interfaces that you need to implement,
- register
- unregister
- getAllServer
- addServerChangeListener

if you test the new feature ok ,please send the feature merge request to the master ,It will be helpful for other people who need it  

## Schedule-Framework
Orion schedule is a high performance distribute task framework ,It was designed  to dispart the schedule module and the task execute module
,because of this ,it can support large scale task's distribute and won't be effected by some task that use more time ,
For the task execute module,it support all kinds of the data for input ,with the virtual group config,the task data can flow each other to 
get a high performance for task execute;

the main logic like this
![logic_pic](https://user-images.githubusercontent.com/66338301/85508700-280ad600-b627-11ea-8dea-a8ac949d045d.png)

the whole feature in the below 

## The main feature
- Task schedule and task execute is divided 

this feature can give ensure for task schedule,only two instances can schedule thousands of the task ,and they will not be deployed,
Compared to the common schedule Quartz,when the task more than the ThreadPool,the task have the risk to be delayed
- Virtual Group 

for the virtual Group, all the instance in the group can share the task data each other,and the ability to process the task data will improved
If you want it will break through the application limited,more than one application can import the logic for the task ,and it can work very well
- All kinds of import task data

for the task ,it has no limit for the task data source,if you want you can read the task data by file/message/db/rpc and so on 
- high concurrence  in each instance

the task module has high concurrence in each instance(10 thread pool default),all task in each instance share the thread pool
- expand scale easy

the scale of the task group can easy to be expand ,only deploy the new instance ,and the task will dispatch to the new instance automatic
Be care of Reduce the scale machine ,if the task was executing,it will loss some wait process data 

- Perceptive of progress 

when the batch of data is deal,the result will be reported to the schedule server,and you can view the progress in the console  
Be care of one thing,the total data of the task will be updated only when all the import stream data read is successful
So you will see the result of the processed data is more than total

- The balance load for all progress 

the task can be distributed more step if you need ,it can help to make more balance between virtual group machine 

- Idempotent

Any batch of the data will be retried three times to the same machine when the dispatch action failed, and the machine who receive the data  
has the mechanism can make sure only one batch will be processed

all the config you can read below [config list](#config list)

## Peformance
- the schedule module can support thousands of task by 2 instance 
- the actual parallel is instance * 10 (10 can be configed by your own application)

## Whole Framework 
![schedule_architure](https://user-images.githubusercontent.com/66338301/85259237-480d8e80-b49b-11ea-8fa3-91749f9a9301.png)
## The main code module
the framework have 4 github code repository ,three for main and one for demo
- schedule-core  
the core implementation for task schedule 
- schedule-console 
an impl for task schedule and offers restful api for task management
- console-web 
the console for the task management ,include virtual group,task management,task instance management
- schedule-demo 
the demo for task development

## How to use it 
###  Compile and Install
- orion-schedule
```bash
git clone https://github.com/orion-open-group/orion-schedule
cd orion-schedule
mvn install -Dmaven.test.skip=true
```
- schedule-server

you should create the datasource to persistent task config and task instance, now it only support mysql ,you can use the init.sql ,it will create database and user 
```bash
git clone https://github.com/orion-open-group/schedule-server
cd schedule-server
mvn install -Dmaven.test.skip=true
java -jar schedule-starter/target/schedule-starter.jar
```
- console-web
the main page for task management,it depends on the schedule console module
```bash
git clone https://github.com/orion-open-group/schedule-web
cd schedule-web
cnpm install
cnpm run dev
``` 
when all is ok ,you can login the console page,the default user is admin/123123

### Config the task
#### Group config

The main logic is create a virtual group,all task will be relatived to a group,it indicate the same code in the group,when it finished,you can copy the group code for your application
#### Task development

to add the dependency schedule-client-starter by the default version
```xml
 <dependency>
    <groupId>com.orion.schedule</groupId>
    <artifactId>schedule-client-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

develop the task by yourself,there three types of task 
- standalone

like the tradition,the task will be executed in one instance,you can inherit the class 
```text
com.orion.schedule.processor.standalone.StandaloneJobProcessor
```
- Distribute Task

the main recommended module,all the task data will be distributed in the virtual group machines
```text
com.orion.schedule.processor.distribute.DistributedJobProcessor
```

- Grid Distribute Task

In this module,task can be distributed more than one times,it the process for one data spend more time, you can use this to reduce the data slope
```text
com.orion.schedule.processor.grid.GridJobProcessor
```
we supposed that ,all the distribute task must solve two things ,where to get the data  to process 
and how to process the data ,most of the time the logic to process the data should involve the logic of our business,it's convenience to abstract two step,fetch data and process data
,when fetch data as a stream,it's more useful to dispatch the data in the group ,and the process logic will be invoke by the framework
- fetchData
- processData

when the task is developed,you can config it in the console page

#### Task Config

it's very simple to config the task in the console page

## <a id="#config list">Config List</a>
```yaml
schedule:
    server:
      codec:
        code: jackson-msg-pack  ## the codec for the task data ,now it only support jackson-msg-pack
      transport:
        code: netty4 ## how to transport the task data ,it's only support netty4
        config:
          serverPort: xxx, ## the server port in the virtual marchine,it used for netty4 to prepare the connection 
          idleTime:  xxx ## how long the netty4 to keey the idle connection
          connectionTimeOut: xxx ## the connection timeout for netty4
          closeWait: xx ## how long the progress to wait for the remian task data to execute,when the application was stoped
      register:
        code: xxx ##the service discover type,now the framework support nacos,zookeeper,etcd,db,eureka
        serverList: ##the server info
           - 10.10.10.10
           - 10.10.10.10
        config:  ## nacos config info
          namespace: xxx ## namespace for nacos
        etcdConfig: ## etcd config info
          ttl: xx ## etcd to keep alive
        zkConfig: ## zk config
          timeout: xx ## zk connection 
        dbConfig: ## dbConfig
          driverClassName: ## the actual driver class that you use for your druid connection
          ttl: xx ## the ttl that the client send heart beat or server decide if the client is alive
        eurekaConfig:
          registerSelf: false ## notice that,if this config as server, it must be false ,and if this is as client, it must be false
      task: 
        groupList:  ## the virtual group info,it comes the group config page
          - xx
          - xx
        processThread: xx, ## the threadpool for task,default value is  10
        noticeType: xx ## how to notify the process result ,not it support DB or KAFKA
        dataSource: ## if the notify type is DB,you should config the next info
          url: xxx  ## the connection url info you should use com.orion.schedule.progress.util.ScheduleEncrypt to keep message safe
          userName: xx ## username for db connection,encrypt like the same
          password:  xx ## the password ,encrypt like the same 
          token: xxx ## json  str contains url,pwd,username ,encrypt like the same 
        messageConfig: ## if the message config is KAFKA,you should config this
          register: xxx ## kafka server info 
          useSSL: xx ## config if use ssl ,default false
          topic: xxx ## the topic 
          useKerberos: xxx ## if use kerberos ,if use please set true
          krbPath: xxxx ## krb file 
          krbTabPath: xxx ## krbTab file path
          kerberosUser: xxx ## kerberors user info 

```

### How to use in schedule server side
if you want to deploy your own server instead of the schedule-console,the config below must be configed 
### Schedule-Server
- codec
- transport
- register
all the config detail you can get reference example [schedule-server](https://github.com/orion-open-group/schedule-server)
### The task execute module
- codec
- transport
- register
- task

### NOTICE
if you use the db as register center ,you should import the table to your db
```sql
'CREATE TABLE `service_alive_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ip` varchar(45) DEFAULT NULL,
  `port` int DEFAULT NULL,
  `group_id` varchar(45) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `ttl_update_time` bigint DEFAULT NULL,
  `status` tinyint(1) DEFAULT ''0'' COMMENT ''0正常，1无效'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8'
```

you can get reverence like this 
[schedule-demo](https://github.com/orion-open-group/schedule-demo)

