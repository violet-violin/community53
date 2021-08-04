package com.ly.community53.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling  //启用Scheduling，就能注入ThreadPoolTaskScheduler了
@EnableAsync  //启用@Async注解；该注解使用见AlphaService#execute1()方法；其可以让被标注的方法在多线程环境下,被异步的调用.
public class ThreadPoolConfig {
}
