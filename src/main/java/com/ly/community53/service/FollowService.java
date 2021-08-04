package com.ly.community53.service;

import com.ly.community53.entity.User;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    public void follow(int userId, int entityType, int entityId) {
        //一次存两个key，保证事务
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                // 存的  某个用户关注的实体   followee:userId:entityType  这里只演示了人——>对应entityType为3
                // 这里的userId，就是登录用户的userId
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                // 某个实体拥有的粉丝 (实体可以是人、课程等；这里只演示了人——对应entityType为3)
                // follower:entityType:entityId -> ZSet(userId,now)
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                // entityId 作为followeeKey ZSet 的元素，当前时间作为score；这样就可以按关注时间查询出 某个用户关注了那些 博主
                // 形参中的 entityId 就是 profile.html页面中 用户的userId
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                // 实体可以是人、课程等；这里只演示了人——对应entityType为3  entityId 就是 用户的userId；
                // 一旦当前用户关注了，就把当前登录用户userId放入这个ZSet
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return operations.exec();
            }
        });
    }

    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().remove(followeeKey, entityId); // 取关，就一处ZSet集合中对应的 实体Id即可
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec();
            }
        });
    }

    // 查询用户关注的目标实体（就是博主）的数量
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey); // .zCard()方法返回ZSet集合大小
    }

    // 查询实体的粉丝的数量
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前登录用户是否已关注该实体
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        //查询entityId的分数，
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    // 查询某用户关注的人的列表(从offset到limit的索引范围；如【0，-1】等)
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        // followee:userId:entityType -> zset(entityId,now)
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        // 按照分数（时间） 从大到小来排，就是 时间越近排前面
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

    // 查询某用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        // follower:entityType:entityId -> zset(userId,now)
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

}
