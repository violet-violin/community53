package com.ly.community53.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling  //启用Scheduling，就能注入ThreadPoolTaskScheduler了
@EnableAsync  //启用@Async注解
public class ThreadPoolConfig {
}
