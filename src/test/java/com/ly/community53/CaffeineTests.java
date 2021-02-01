package com.ly.community53;

import com.ly.community53.entity.DiscussPost;
import com.ly.community53.service.DiscussPostService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CaffeineTests {

    @Autowired
    private DiscussPostService postService;


    //等会要做压测，故在这里，先初始化数据库的数据300000条
    @Test
    public void initDataForTest() {
        for (int i = 0; i < 300 /*300000*/; i++) {  //往数据库插300000条数据
            DiscussPost post = new DiscussPost();
            post.setUserId(111);
            post.setTitle("互联网求职暖春计划");
            post.setContent("今年的就业形势，确实不容乐观。过了个年，仿佛跳水一般，整个讨论区哀鸿遍野！19届真的没人要了吗？！18届被优化真的没有出路了吗？！大家的“哀嚎”与“悲惨遭遇”牵动了每日潜伏于讨论区的牛客小哥哥小姐姐们的心，于是牛客决定：是时候为大家做点什么了！为了帮助大家度过“寒冬”，牛客网特别联合60+家企业，开启互联网求职暖春计划，面向18届&19届，拯救0 offer！");
            post.setCreateTime(new Date());
            post.setScore(Math.random() * 2000);
            postService.addDiscussPost(post);
        }
    }

    //测试方法，用于测试缓存；查行数时跟这个是一样的
    @Test
    public void testCache() {
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));//第一次就是去数据库取值
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));//第二次就能去缓存取了
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));//第二次就能去缓存取了
        System.out.println(postService.findDiscussPosts(0, 0, 10, 0));//这种不走缓存
    }

}
