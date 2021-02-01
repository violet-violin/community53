package com.ly.community53;

import com.ly.community53.entity.DiscussPost;
import com.ly.community53.service.DiscussPostService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SpringBootTests {

    @Autowired
    private DiscussPostService discussPostService;

    private DiscussPost data;

    @BeforeClass//类初始化之前，该注解标注的方法会执行；只执行一次
    public static void beforeClass() {
        System.out.println("beforeClass");
    }

    @AfterClass//类销毁之后，该注解标注的方法会执行；只执行一次
    public static void afterClass() {
        System.out.println("afterClass");
    }

    @Before//调用任何一个测试方法前，该注解标注的方法会执行
    public void before() {
        System.out.println("before");

        // 初始化测试数据
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("Test Title");
        data.setContent("Test Content");
        data.setCreateTime(new Date());
        discussPostService.addDiscussPost(data);
    }

    @After//调用任何一个测试方法之后，该注解标注的方法会执行
    public void after() {
        System.out.println("after");

        // 删除测试数据
        discussPostService.updateStatus(data.getId(), 2);//2——就是删除帖子
    }

    @Test
    public void test1() {
        System.out.println("test1");
    }

    @Test
    public void test2() {
        System.out.println("test2");
    }

    @Test
    public void testFindById() {//测试findDiscussPostById方法
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertNotNull(post); //断言的方式
        Assert.assertEquals(data.getTitle(), post.getTitle());
        Assert.assertEquals(data.getContent(), post.getContent());
    }

    @Test
    public void testUpdateScore() {//测试updateScore方法
        int rows = discussPostService.updateScore(data.getId(), 2000.00);
        Assert.assertEquals(1, rows);

        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertEquals(2000.00, post.getScore(), 2);//delta表示精度，只比较两位小数之前是否相等
    }

}
