# spring cloud 学习

<h2><a name="eureka" style="text-decoration:none">一.  Eureka服务治理体系</a></h2>

服务治理是微服务架构中最为核心和基础的模块，它主要用来实现各个微服务实例的自动化注册和发现。

Spring Cloud Eureka是Spring Cloud Netflix微服务套件中的一部分，它基于Netflix Eureka做了二次封装。主要负责完成微服务架构中的服务治理功能。

Eureka的三个核心角色：服务注册中心、服务提供者和服务消费者,前一个属于Eureka server端, 后两个属于Eureka client

### 基础架构

**服务注册中心:** Eureka server 即 Eureka服务端，提供服务注册与发现的功能,具有以下几个功能

1. **服务下线**

   在客户端程序中，当服务实例进行正常的关闭操作时，它会出发一个服务下线的REST请求给Eureka Server，告诉服务注册中心“我要下线了”。服务端在接受到请求后，讲该服务状态置为下线并把该下线事件传播出去

2. **失效剔除**

   有时服务实例不是正常下线的，而服务注册中心并未收到“服务下线”的请求。为了从服务列表讲这些无法提供服务的实例剔除，Eureka Server在启动的时候会创建一个定时任务，默认每隔60秒, 将当前清单中超时(默认90秒)没有续约的服务剔除出去。

3. **自我保护**

   在服务注册中心的信息面板中出现类似下面的红色警告信息：

   ![技术分享图片](http://image.mamicode.com/info/201712/20180111010516773769.png)

   实际上，该警告就出发了Eureka Server的自我保护机制。Eureka Server在运行期间，会统计心跳失败的比例在15分钟之内是否大于15%，如果出现大于的情况Eureka Server会将当前实例注册信息保护起来，让这些实例不会过期，尽可能保护这些注册信息。

   在本地调试的时候很容易触发注册中心的保护机制，这会使得注册中心维护的服务实例不那么准确。所以我们再本地进行开发的时候，可以通过设置参数

   `eureka.server.enable-self-preservation=false` 来关闭保护机制，保证注册中心将不可用的实例正确剔除。

**服务提供者:** 提供服务的应用，可以是Spring Boot应用，也可以是其他技术平台且遵循Eureka通信机制的应用。它将自己提供的服务注册到Eureka，以供其他应用发现。

* **服务注册:**  在服务注册时，需要确认一下eureka.client.registerwith-eurek=ture参数是否正确，默认是true，若设置为false将不会启动注册操作。

* **服务同步:**  两个服务提供者的服务信息是一样的

* **服务续约:**  主要看两个主要属性

  ```properties
  #定义服务续约任务的调用间隔时间，默认为30秒
  eureka.instance.lease-renewal-interval-in-seconds=30
  #定义服务失效的时间，默认为90秒
  eureka.instance.lease-expiration-duration-in-secodes=90
  ```

**服务消费者:** 消费者应用从服务注册中心获取服务列表，从而使消费者可以知道去何处调用所需要的服务。

1. **获取服务**

   当服务消费者启动的时候，会发送一个REST请求给服务注册中心，来获取服务注册清单。为了性能考虑，Eureka Server会维护一份只读的服务清单来返回给客户端，同时该缓存清单会每隔30秒更新一次。获取服务是服务消费者的基础，所以必须确保 `eureka.client.fetch-registry=ture` (该值默认是true)。可以通过 `eureka.client.registry-fetch-interval-seconds=30` 参数修改缓存清单的更新时间。

 2. **服务调用**

    服务消费者在获取的服务清单后，通过服务名可以获得具体提供服务的实例名和该实例的元数据信息。因为有这些服务实例的详细信息，所以客户端可以根据自己的需要决定具体调用哪个实例，在`Ribbon`中会默认采用轮询的方式进行调用，从而实现客户端的负载均衡。对于访问实例选择，Eureka中有Region和Zone的概念，一个Region中可以包含多个Zone，每个服务客户端需要被注册到一个Zone中，所以每个客户端对应一个Region和一个Zone。

### 示例代码

本示例展示如何搭建服务注册中心以及各应用如何向注册中心注册自己的服务

### 一  搭建Eureka Server 注册中心

新建一个maven项目,并添加如下配置

**pom中添加**

```xml
<!--添加eureka服务端依赖-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

**配置文件**

```properties
#端口号
server.port=8761
#应用名称
spring.application.name=eureka-server
#关于eureka server端的配置
#服务注册中心实例的主机名
eureka.instance.hostname=localhost
#是否向服务注册中心注册自己
eureka.client.register-with-eureka=false
#是否检索服务
eureka.client.fetch-registry=false
#关闭服务的自我保护,用于开发测试,生产时该服务需要设置为true,默认值为true
eureka.server.enable-self-preservation=false
#spring.freemarker.prefer-file-system-access=false
#服务注册中心地址,如果有多个注册中心, 使用,分割
eureka.client.serviceUrl.defaultZone=http://${eureka.instance.hostname}:${server.port}/eureka/
```

**启动类配置**

```java
@SpringBootApplication
@EnableEurekaServer //启动Eureka 服务端
public class EurekaServerApplication {
    public static void main(String[] args){
        SpringApplication.run(com.eureka.server.EurekaServerApplication.class,args);
    }
}
```

**启动注册中心**

启动应用成功后, 通过浏览器访问: http://localhost:8761/ 可以访问注册中心可视化页面,如下图所示,  **表示服务注册中心搭建成功**

![注册中心](/imgs/注册中心.png)

### 二  服务注册--Eureka client 客户端

本示例提供两个demo, **服务提供者**用于向外提供服务, **服务消费者**用于消费服务

### <1> 服务提供者

在spring cloud中,远程服务是通过`http`协议, 以http接口方式向外提供服务

新建一个maven子项目, 使用mysql+jpa方式, 通过简单的数据查询, 向外提供简单的学生信息查询服务,提供的服务接口为 `/findAll`

关于jpa的知识可以查看下列链接:

<a href="https://www.jianshu.com/p/c23c82a8fcfc" style="color:blue;text-decoration:none">Spring Data JPA使用</a>

<a href="https://www.tianmaying.com/tutorial/spring-jpa-custom-all" style="color:blue;text-decoration:none">Spring Data JPA: 为所有Repository添加自定义方法</a>

<a href="https://www.w3cschool.cn/java/jpa-entitymanager.html" style="color:blue;text-decoration:none">w3c jpa详解</a>

**pom中添加**

```xml
<dependencies>
    <!--mysql 和 jpa-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>5.1.38</version>
    </dependency>

    <!--eureka client依赖-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

</dependencies>
```

**配置文件**

```properties
#客户端应用端口
server.port=8081
#应用名称,用于服务的发现,当该服务注册成功后,在注册中心将显示该名称
spring.application.name=eureka-service-provider

#mysql配置
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/test?characterEncoding=utf8&useSSL=true
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.MySQL5Dialect

#指定服务注册中心的位置,当有多个注册中心时, 地址使用,分割
eureka.client.serviceUrl.defaultZone=http://127.0.0.1:8761/eureka/
#多配置中心配置
#eureka.client.serviceUrl.defaultZone=http://127.0.0.1:8761/eureka/,http://127.0.0.1:8762/eureka/,
```

**启动类配置**

```java
@SpringBootApplication
@EnableEurekaClient //启动eureka客户端
@EnableJpaRepositories//启动jpa支持
public class EurekaProviderApplication {
    public static void main(String[] args){
        SpringApplication.run(EurekaProviderApplication.class,args);
    }
}
```

**提供的服务**

```java
@RestController
public class StudentController {

    @Value("${server.port}")
    private String port;//应用启动端口

    @GetMapping("/findAll")
    public ResponseEntity findAll(){
        System.out.println("通过端口:"+port+" 查询所有学生信息");
        Iterable<Student> iterable = studentDao.findAll();
        ResponseEntity entity = new ResponseEntity(iterable,HttpStatus.OK);
        return entity;
    }
    @Autowired
    private StudentDao studentDao;
}
```

**实体映射类**

```java
@Entity
@Table
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column
    private String name;
    @Column
    private int age;
    //getter setter方法
}
```

```java
public interface StudentDao extends PagingAndSortingRepository<Student,Long> {
}
```



### <2> 服务消费者

本示例通过`RestTemplate`来调用其他微服务,并通过`Ribbon`进行负载均衡

关于RestTemplate和Ribbon的知识可以查看下列链接:

<a href="https://www.zifangsky.cn/1221.html" style="color:blue;text-decoration:none">使用RestTemplate访问REST服务详解</a>

<a href="https://zhuanlan.zhihu.com/p/31681913" style="color:blue;text-decoration:none">RestTemplate 详解</a>

<a href="https://www.xncoding.com/2017/07/06/spring/sb-restclient.html" style="color:blue;text-decoration:none">SpringBoot系列 - 使用RestTemplate</a>

<a href="https://www.itcodemonkey.com/article/12927.html" style="color:blue;text-decoration:none">Spring Cloud：使用Ribbon实现负载均衡详解（上）</a>

<a href="https://www.itcodemonkey.com/article/12926.html" style="color:blue;text-decoration:none">Spring Cloud：使用Ribbon实现负载均衡详解（下）</a>

<a href="http://blog.didispace.com/springcloud-sourcecode-ribbon/" style="color:blue;text-decoration:none">Spring Cloud源码分析: Ribbon</a>

**pom中添加**

```xml
<dependencies>
   <!--eureka客户端依赖,用于注册服务-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!--集成ribbon,作为负载均衡-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
    </dependency>
</dependencies>
```

**配置文件**

```properties
#客户端应用端口
server.port=8083
#应用名称,用于服务的发现,当该服务注册成功后,在注册中心将显示该名称
spring.application.name=eureka-ribbon-consumer

#指定服务注册中心的位置,当有多个注册中心时, 地址使用,分割
eureka.client.serviceUrl.defaultZone=http://127.0.0.1:8761/eureka/
```

**启动类配置**

```java
@SpringBootApplication
@EnableEurekaClient //开启eureka服务注册
public class EurekaRibbonConsumerApplication {
    public static void main(String[] args){
        SpringApplication.run(EurekaRibbonConsumerApplication.class,args);
    }
}
```

**配置类**

配置类主要用于开启Ribbon的负载均衡功能

```java
@Configuration
public class RibbonConfig {
    @Bean
    @LoadBalanced //开启ribbon负载均衡
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
```

**服务消费**

通过`RestTemplate` 直接发起http调用请求, 调用eureka-service-provider应用(服务提供者)提供的服务

```java
@RestController
public class RibbonController {

    @Autowired
    private RestTemplate template;

    @GetMapping("/getInfo")
    public ResponseEntity getStudentInfo(){
        /**
         *   ribbon能直接通过应用名称找到应用对应的ip和端口,如果应用是通过集群搭建,
         *  那么ribbon会通过轮询的方式向该应用不同的服务器上发送请求以达到负载均衡的功能
         *  本次功能是向eureka-service-provider应用(服务提供者)发起查询请求
         */
        ResponseEntity entity = template.getForEntity("http://eureka-service-provider/findAll",ResponseEntity.class);
        return entity;
    }
}
```

**启动项目**

先启动服务注册中心`eureka-server` ,再分别启动 服务提供者`eureka-service-provider`和服务消费者`eureka-ribbon-consumer`

通过浏览器访问 http://localhost:8761/访问注册中心可视化界面,如下图所示

红框部分表示 服务提供者和服务消费者在注册中心注册成功:

![provider注册](/imgs/服务注册.png)

通过浏览器访问`eureka-ribbon-consumer`服务消费端的接口(http://localhost:8083/getInfo)结果如下:

![查询结果](E:\spring-learn-all\spring-cloud-learn\imgs\查询结果.png)

控制台打印如下图所示:

![kzt1](/imgs/kzt1.png)

> 该结果无法体现Ribbon的负载均衡功能,原因是服务提供者`eureka-service-provider`只启动了一个应用实例,下面我们启动多个应用示例.

**服务提供者多应用实例启动**

在`eureka-service-provider`提供两份配置文件 `application-pro1.properties`,`application-pro2.properties`,两个配置文件中配置项除了 `server.port`(一个端口配置为8081,一个端口配置为8082) 其他属性都一样,然后在`Idea`中使用`Maven`指定配置文件启动应用的不同实例,配置如下图所示:

![dpd](/imgs/dqd1.png)

![dqd2](/imgs/dpd2.png)

分别启动项目,通过浏览器访问`eureka-ribbon-consumer`服务消费端的接口(http://localhost:8083/getInfo), 控制台打印如下图所示:

![kzt1](/imgs/kzt1.png)

![kzt1](/imgs/kzt2.png)

上图可以看出不断刷新浏览器请求, Ribbon会将消费端请求通过轮询的方式转发到不同的服务提供者的服务器上,以达到负载均衡的目的.

### Eureka 配置详解

> `eureka.server.*`的配置是`org.springframework.cloud.netflix.eureka.server.EurekaServerConfigBean`在处理
>
> `eureka.instance.*`的配置是`org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean`在处理
>
> `eureka.client.*`的配置是 `org.springframework.cloud.netflix.eureka.EurekaClientConfigBean` 在处理
>
> 可以通过Maven打开对应的类查看具体的对应配置内容

**常用配置**

```properties
# 设置本实例是否注册到服务注册中心，因为有些时候实例只想获取服务而不想提供服务
eureka.client.register-with-eureka=true/false（默认为true）

# 设置本客户端是否从服务注册中心获取服务
eureka.client.fetch-registry=true/false（默认为true）

# 设置注册的服务多久向服务注册中心发送心跳包
eureka.instance.lease-renewal-interval-in-seconds=30（单位是 s）

# 设置多久没有收到注册服务的心跳包后剔除该服务
eureka.instance.lease-expiration-duration-in-seconds=90（单位是 s）


# 设置服务是否开启保护机制，即使eureka.instance.lease-expiration-duration-in-seconds超时也不会剔除该服务，一直等待服务重新开启，设置true时会一直持有该服务不释放(不释放的前提是Eureka Server在运行期间统计心跳失败的比例在15分钟之内是否大于15%，如果出现大于的情况Eureka Server会将当前实例注册信息保护起来，让这些实例不会过期，尽可能保护这些注册信息。)
eureka.server.enable-self-preservation=ture/false（默认为true）

# 设置指定注册服务中心地址，如果查看源码就可以发现，serviceUrl的配置存储在Map类型中，其中key是Zone这里是defaultZone，value为具体的URL地址这里是http://localhost:1111/eureka/，所以也可以配置其它的Zone
eureka.client.serviceUrl.defaultZone=http://localhost:1111/eureka/,http://localhost:1112/eureka/

#查看源码可以看到availabilityZones是一个HashMap类型，其中key是region，value是用,隔开的zones
eureka.client.availability-zones.*
# 所以自定义zones需要如下所示
eureka.client.region=love
eureka.client.availabilityZones.love=mlq,roye,fly

```

关于Eureka的其他配置可以参考下列文章:

<a href="https://zhuanlan.zhihu.com/p/52454663" style="color:blue;text-decoration:none">Spring Cloud -- Eureka 配置详解</a>

<a href="https://blog.csdn.net/qq_26440803/article/details/83113037" style="color:blue;text-decoration:none">Eureka配置信息详解</a>

<a href="https://www.jianshu.com/p/30bf1ba78107" style="color:blue;text-decoration:none">spring-cloud | Eureka参数配置项详解</a>

