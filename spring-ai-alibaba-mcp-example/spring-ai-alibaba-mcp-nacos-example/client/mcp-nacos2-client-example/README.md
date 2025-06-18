目前1.0.0.2版本的的不支持配置自定义的命名空间，1.0.0.3-SNAPSHOT进行了修复

### 依赖
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-nacos2-mcp-client</artifactId>
</dependency>
```

### application.yml
```yml
server:
  port: 8080

spring:
  application:
    name: mcp-nacos2-client-example
  main:
    web-application-type: none
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com
      chat:
        options:
          model: qwen-max
    mcp:
      client:
        enabled: true
        name: my-mcp-client
        version: 1.0.0
        request-timeout: 30s
        type: ASYNC  # or ASYNC for reactive applications

    alibaba:
      mcp:
        nacos:
          enabled: true
          server-addr: 127.0.0.1:8848
          namespace: 9ba5f1aa-b37d-493b-9057-72918a40ef35
          username: nacos
          password: nacos

        client:
          enabled: true
          sse:
            connections:
              server1:
                service-group:  mcp-server
                service-name: webflux-mcp-server

```
开启配置：
- enabled: true

这里需要注意配置 namespace，若不配置则默认使用public
