package com.ly.community53.service;

import com.ly.community53.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author malaka
 * @create 2020-12-24 14:26
 */
@Service//标上此注解，会被ioc调用构造方法生成bean
//@Scope("prototype")//默认参数是“singleton”；"prototype"代表bean不是单例；基本上都是单例
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    public AlphaService() {
        System.out.println("实例化AlphaService");
    }

    @PostConstruct//构造器后调用
    public void init() {
        System.out.println("初始化AlphaService");
    }

    @PreDestroy//销毁构造器后调用
    public void destroy() {
        System.out.println("销毁AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }

}
