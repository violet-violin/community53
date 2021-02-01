package com.ly.community53.config;

import com.ly.community53.quartz.AlphaJob;
import com.ly.community53.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 配置 -> 数据库 -> 调用
@Configuration
public class QuartzConfig {

    // FactoryBean可简化Bean的实例化过程:
    // 1.通过FactoryBean封装Bean的实例化过程.
    // 2.将FactoryBean装配到Spring容器里.
    // 3.将FactoryBean注入给其他的Bean.
    // 4.该其他的Bean会得到的是FactoryBean所管理的对象实例————即factory生成的对象实例。

    // 配置JobDetail
//     @Bean          QuartzConfig 这个类执行一次就行了，执行一次就注掉@Bean
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);//设置Jog实例
        factoryBean.setName("alphaJob");//给Job的名字
        factoryBean.setGroup("alphaJobGroup");//组名
        factoryBean.setDurability(true);//任务长久保存
        factoryBean.setRequestsRecovery(true);//任务可恢复
        return factoryBean;
    }

    // 配置Trigger(SimpleTriggerFactoryBean, CronTriggerFactoryBean(复杂trigger，有特殊表达式完成复杂逻辑，如每周二晚上10点做什么))
//     @Bean   QuartzConfig 这个类执行一次就行了，执行一次就注掉@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) { //alphaJobDetail和上面方法名对应，是factory生成实例的名字
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");//trigger的名字
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000);//频率3000ms
        factoryBean.setJobDataMap(new JobDataMap());// 啥？ 初始化一个JobDataMap存储job的状态
        return factoryBean;
    }


    // 刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5);//频率5分钟
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

}
