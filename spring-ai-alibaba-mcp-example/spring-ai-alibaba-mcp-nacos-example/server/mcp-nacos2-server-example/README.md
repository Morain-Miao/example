目前1.0.0.2版本的的不支持配置自定义的命名空间，1.0.0.3-SNAPSHOT进行了修复

### 依赖
```xml
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-starter-nacos2-mcp-server</artifactId>
        </dependency>
```

### application.yml
```yml
server:
  port: 21000

spring:
  main:
    banner-mode: off
  application:
    name: mcp-nacos2-server-example
  ai:
    mcp:
      server:
        name: webflux-mcp-server
        version: 1.0.0
        type: ASYNC  # Recommended for reactive applications
        instructions: "This reactive server provides time information tools and resources"
        sse-message-endpoint: /mcp/messages
        capabilities:
          tool: true
          resource: true
          prompt: true
          completion: true
    alibaba:
      mcp:
        nacos:
          namespace: 9ba5f1aa-b37d-493b-9057-72918a40ef35
          enabled: true
          server-addr: 127.0.0.1:8848
          username: nacos
          password: nacos
          registry:
            enabled: true
            service-group: mcp-server
```
这里需要注意配置namespace，若不配置则默认使用public
