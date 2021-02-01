package com.ly.community53.controller;

import com.ly.community53.entity.*;
import com.ly.community53.event.EventProducer;
import com.ly.community53.service.CommentService;
import com.ly.community53.service.DiscussPostService;
import com.ly.community53.service.LikeService;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.HostHolder;
import com.ly.community53.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

/**
 * @author malaka
 * @create 2021-01-05 21:29
 */
@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    //添加评论的方法
    //这个discussPostId从哪儿提交过来呢？？  post，其有一个提交的路径：th:action="@{|/comment/add/${post.id}|}"
    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        //todo 后面会做权限认证

        comment.setUserId(hostHolder.getUser().getId());//如果用户没有登录，就会报错
        comment.setStatus(0);   //status，何意？？  0代表正常的评论；1代表被禁用的评论
        comment.setCreateTime(new Date());  //comment的其他字段，是在discuss-detail的评论提交页面被赋值的并封装上的
        //comment的entityType、entityId呢？ 不添加？？   post的参数直接封装

        commentService.addComment(comment);

        // 添加评论后，就触发评论事件了
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {//评论的是帖子
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());//target.getUserId() ———— 帖子作者id
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {//评论的是评论，即是回复。
            Comment target = commentService.findCommentById(comment.getEntityId());//comment.getEntityId()是评论的id
            event.setEntityUserId(target.getUserId());//target.getUserId()发布评论的作者id
        }
        //生产者处理事件，消费者一有内容就会自动触发；然后就会发生系统通知（该功能后续马上实现）
        eventProducer.fireEvent(event);

        //一旦帖子有了评论，discuss_post表的comment_count字段就会加1，相应的触发评论事件，更新es。
        if(comment.getEntityType() == ENTITY_TYPE_POST){//评论的是帖子
            // 触发发帖事件
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);//生产者发布事件

            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId); //set类型，自动去重
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }

    @RequestMapping(path = "/myComments", method = RequestMethod.GET)
    public String getCommentByUserId(Model model, Page page) {

        // todo 这个应该拦截器检查是否登录的。。。。  不然直接输入url就能访问
        User user = hostHolder.getUser();
        // 方法调用钱,SpringMVC会自动实例化Model和Page,并将Page注入Model.// 所以,在thymeleaf中可以直接访问Page对象中的数据.
        // why？？page的current属性信息什么时候从页面封装入page对象的？自动封装？——对  page的current、limit有默认值

        if (user == null) {
            throw new RuntimeException("未登录!");//或是重定向，return "redirect:/login";
        }
        page.setLimit(5);
        //setRows()————数据总数(用于计算总页数)  这里代表该user的总发帖数
        page.setRows(commentService.findCommentCountByUserId(user.getId()));
        page.setPath("/comment/myComments");   //path是   查询路径(用于复用分页链接)

        int myCommentRows = commentService.findCommentCountByUserId(user.getId());
        model.addAttribute("myCommentRows",myCommentRows);

        //这是从数据库取的userId的总的发的评论/回复
        List<Comment> list = commentService.findCommentsByUserId(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> myComments = new ArrayList<>();//这里存储给页面的数据
        int entityId = 0;  //entityId ———— 指向帖子的id
        if (list != null) {
            for (Comment comment : list) {
                Map<String, Object> map = new HashMap<>();
                //评论/回复存入，数据库中帖子的字段包括：id、user_id、entity_type、entity_id、target_id、content、status、create_time
                map.put("comment", comment);
                if(comment.getEntityType() == ENTITY_TYPE_POST){
                    entityId = comment.getEntityId();
                }else if(comment.getEntityType() == ENTITY_TYPE_COMMENT){
                    entityId =   commentService.findEntityIdById(comment.getEntityId());
                }

                //根据帖子的id查出帖子
                DiscussPost discussPostById = discussPostService.findDiscussPostById(entityId);
                map.put("discussPostById",discussPostById);

//                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, comment.getEntityId()) +
//                        likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getEntityId());
//                map.put("likeCount", likeCount);  //看页面需不需要这个  ————不需要

                myComments.add(map);
            }
        }
        model.addAttribute("myComments", myComments);
        return "/site/my-reply";
    }



}
