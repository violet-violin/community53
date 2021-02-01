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

    //todo 可以自己使用 拦截器 实现，没有登录就不能使用点赞的异步请求功能


    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody      //异步请求都返回json吗？
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();

        // 点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        // 数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        // 返回的结果
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
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return CommunityUtil.getJSONString(0, null, map);//把赞的数量、状态给返回
    }
}
