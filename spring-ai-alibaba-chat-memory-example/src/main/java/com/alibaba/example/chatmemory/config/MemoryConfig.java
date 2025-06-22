package com.alibaba.example.chatmemory.config;

import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;
import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Configuration
public class MemoryConfig {

	@Value("${spring.ai.memory.redis.host}")
	private String redisHost;
	@Value("${spring.ai.memory.redis.port}")
	private int redisPort;
	@Value("${spring.ai.memory.redis.password}")
	private String redisPassword;
	@Value("${spring.ai.memory.redis.timeout}")
	private int redisTimeout;

	@Value("${spring.ai.chat.memory.repository.jdbc.mysql.jdbc-url}")
	private String mysqlJdbcUrl;
	@Value("${spring.ai.chat.memory.repository.jdbc.mysql.username}")
	private String mysqlUsername;
	@Value("${spring.ai.chat.memory.repository.jdbc.mysql.password}")
	private String mysqlPassword;
	@Value("${spring.ai.chat.memory.repository.jdbc.mysql.driver-class-name}")
	private String mysqlDriverClassName;

	@Bean
	public SQLiteChatMemoryRepository sqliteChatMemoryRepository() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.sqlite.JDBC");
		dataSource.setUrl("jdbc:sqlite:spring-ai-alibaba-chat-memory-example/src/main/resources/chat-memory.db");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return SQLiteChatMemoryRepository.sqliteBuilder()
				.jdbcTemplate(jdbcTemplate)
				.build();
	}

	@Bean
	public MysqlChatMemoryRepository mysqlChatMemoryRepository() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(mysqlDriverClassName);
		dataSource.setUrl(mysqlJdbcUrl);
		dataSource.setUsername(mysqlUsername);
		dataSource.setPassword(mysqlPassword);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return MysqlChatMemoryRepository.mysqlBuilder()
				.jdbcTemplate(jdbcTemplate)
				.build();
	}

	@Bean
	public RedisChatMemoryRepository redisChatMemoryRepository() {
		return RedisChatMemoryRepository.builder()
				.host(redisHost)
				.port(redisPort)
				// 若没有设置密码则注释该项
//				.password(redisPassword)
				.timeout(redisTimeout)
				.build();
	}
}
