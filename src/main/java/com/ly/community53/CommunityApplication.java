package com.ly.community53;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

//@MapperScan("com.ly.community.dao")
@SpringBootApplication
public class CommunityApplication {

    @PostConstruct//该注解用于bean管理，在bean的构造器执行完成后执行。
    public void init() {
        // 解决netty启动冲突问题  es、redis底层都是native，两者底层会有冲突，主要是es底层的冲突。
        //NettyRuntime的setAvailableProcessors方法、Netty4Utils的setAvailableProcessors方法冲突
        // see Netty4Utils.setAvailableProcessors()
        //把该属性设置为false，则Netty4Utils.setAvailableProcessors()方法就不会执行后续的检查
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }

}
