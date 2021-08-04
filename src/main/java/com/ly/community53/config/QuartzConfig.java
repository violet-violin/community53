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
    // 4.其他的Bean会得到的是FactoryBean所管理的对象实例 —> 即factory生成的对象实例（如JobDetailFactoryBean管理的JobDetail对象实例）。
    // 如注入一个JobDetailFactoryBean，你会得到JobDetail的bean。如下面一段代码

    // 配置JobDetail
//     @Bean          QuartzConfig 这个类执行一次就行了，执行一次就注掉@Bean。
//     配置好后，quartz就会读取配置信息，把读到的配置信息存储到DB表里；
//     以后就读取表来执行任务，配置初始化到DB后，就不再用到。
//     （这需要先进行properties里quartz的配置才会这样，不进行properties里的配置就只是读取内存里的jobDetail、Trigger来执行定时任务，就不能注掉）
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);//设置Job实例
        factoryBean.setName("alphaJob");//给Job的名字
        factoryBean.setGroup("alphaJobGroup");//组名
        factoryBean.setDurability(true);//任务长久保存
        factoryBean.setRequestsRecovery(true);//任务可恢复
        return factoryBean;
    }

    // 配置Trigger(SimpleTriggerFactoryBean [简单的trigger], CronTriggerFactoryBean[复杂trigger，有特殊表达式完成复杂逻辑，如每周二晚上10点做什么])
//     @Bean   QuartzConfig 这个类执行一次就行了，执行一次就注掉@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) { //alphaJobDetail和上面方法名对应，是factoryBean所生成实例jobDetail的名字
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");//trigger的名字
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000);//频率3000ms
        factoryBean.setJobDataMap(new JobDataMap());// 啥？ 初始化一个JobDataMap存储job的状态
        return factoryBean;
    }


    // 刷新帖子分数 定时任务
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
