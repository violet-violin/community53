package com.ly.community53.controller;

import com.ly.community53.entity.Event;
import com.ly.community53.entity.User;
import com.ly.community53.event.EventProducer;
import com.ly.community53.service.LikeService;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.CommunityUtil;
import com.ly.community53.util.HostHolder;
import com.ly.community53.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * @author malaka
 * @create 2021-01-07 11:21
 */
@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    //todo 可以自己使用 拦截器 实现，没有登录就不能使用点赞的异步请求功能； 对这里要做，不然就获取不到userId，会报错


    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody      //异步请求都返回json吗 ——> 对，ajax 的post请求 就会返回要给 data，里面就是json数据
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();

        // 点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        // 数量：查询某实体（帖子/评论/回复）点赞的数量，因为点完/取消赞后这个数量会加一
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 状态：查询某人对某实体的点赞状态，某人是否给某实体点过赞；因为点完/取消赞后这个状态会变
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        // 返回的结果，会 map 会转为json 发送回 ajax 前端
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 触发点赞事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId()) //当前用户点的赞
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId); //把帖子id放入data里
            eventProducer.fireEvent(event);
        }

        if(entityType == ENTITY_TYPE_POST){
            // 计算帖子分数   就是把postId存入 post:score 这个key里，用到set；后续计算分数时将点赞数带入一个公式计算分值
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return CommunityUtil.getJSONString(0, null, map);  //把赞的数量、状态给返回
    }
}
