package com.ly.community53.service;

import com.ly.community53.dao.AlphaDao;
import com.ly.community53.dao.DiscussPostMapper;
import com.ly.community53.dao.UserMapper;
import com.ly.community53.entity.DiscussPost;
import com.ly.community53.entity.User;
import com.ly.community53.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

/**
 * @author malaka
 * @create 2020-12-24 14:26
 */
@Service//标上此注解，会被ioc调用构造方法生成bean
//@Scope("prototype")//默认参数是“singleton”；"prototype"代表bean不是单例；基本上都是单例
public class AlphaService {

    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);

    @Autowired
    private AlphaDao alphaDao;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    //TransactionTemplate 这是spring自己注入容器的
    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
//        System.out.println("实例化AlphaService");
    }

    @PostConstruct//构造器后调用
    public void init() {
//        System.out.println("初始化AlphaService");
    }

    @PreDestroy//销毁构造器后调用
    public void destroy() {
//        System.out.println("销毁AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }


    // spring事务管理：声明式事务demo
    //    - 通过XML配置，声明某方法的事务特征。
    //    - 通过注解，声明某方法的事务特征。@Transactional，声明两个参数，事务隔离级别、事务传播机制

    //    REQUIRED(0),
    //    SUPPORTS(1),
    //    MANDATORY(2),
    //    REQUIRES_NEW(3),
    //    NOT_SUPPORTED(4),
    //    NEVER(5),
    //    NESTED(6);
    // REQUIRED: 支持当前事务(外部事务),如果不存在则创建新事务.
    // REQUIRES_NEW: 创建一个新事务,并且暂停当前事务(外部事务).
    // NESTED: 如果当前存在事务(外部事务),则嵌套在该事务中执行(独立的提交和回滚),否则就会REQUIRED一样.
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        // 新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("Hello");
        post.setContent("新人报道!");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);

        Integer.valueOf("abc");//故意报错的语句；"123" 的形参才能正确执行

        return "ok";
    }

    // 编程式事务demo
    //    - 通过 TransactionTemplate 管理事务，并通过它执行数据库的操作。
    public Object save2() {
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                // 新增用户
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("beta@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/999t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                // 新增帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("你好");
                post.setContent("我是新人!");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);

                Integer.valueOf("abc");//故意报错的语句

                return "ok";
            }
        });
    }


    @Async  //可以让该方法在多线程环境下,被异步的调用.
    public void execute1() {
        logger.debug("execute1");
    }

    //让该方法延时10s执行，定时1s执行；只要项目中有方法在跑，该方法会自动去调用
    //这是个定时任务，一启动程序就触发，故注掉
//    @Scheduled(initialDelay = 10000, fixedRate = 1000)
//    public void execute2() {
//        logger.debug("execute2");
//    }
}
