package com.ly.community53.service;

import com.ly.community53.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

/**
 * @author malaka
 * @create 2021-01-07 10:05
 */
@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞；userId是用户id，entityType是点赞实体类型，entityId是点赞实体的id。  //entityUserId是实体的作者id，如帖子作者id
    // private int entityType;  //评论/点赞的目标类型 1-帖子；2-评论/回复；3-课程；之类  // Comment类的两个属性
    // private int entityId;  //评论/点赞的实体(如帖子/评论)的id；entityType=1 时，entityId为帖子id，entityType=2时，entityId为评论id
    public void like(int userId, int entityType, int entityId, int entityUserId) {
//        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
//        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
//        if (isMember) {
//            redisTemplate.opsForSet().remove(entityLikeKey, userId);//取消赞
//        } else {
//            redisTemplate.opsForSet().add(entityLikeKey, userId);//点赞
//        }

        //重构，用redis的编程式事务
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //某个实体的赞；like:entity:entityType:entityId -> set(userId)
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                // 该key用于统计某个用户的获得的所有赞，该key以userId作唯一标识；like:user:userId -> int
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                // isMember() 方法代表 userId 是否是 entityLikeKey 这个 set 集合里的元素之一
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

                operations.multi();

                if (isMember) {//这两条用事务做
                    // key 是 like:entity:entityType:entityId，值是 当前登录用户userId
                    operations.opsForSet().remove(entityLikeKey, userId);//取消赞，已经存在了userId，你再点，就是消赞了
                    operations.opsForValue().decrement(userLikeKey);
                } else {
                    operations.opsForSet().add(entityLikeKey, userId);//点赞
                    // 一旦有人点赞，就要增加 这个实体对应作者的 所有点赞数，这个会在用户个人主页用到，显示总共收到多少赞
                    operations.opsForValue().increment(userLikeKey);
                }

                return operations.exec();
            }
        });
    }

    // 查询某实体（帖子/评论/回复）点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        //某个实体的赞；like:entity:entityType:entityId -> set(userId)
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey); // .size()方法代表entityLikeKey 这个set集合元素个数，即多少个用户点了赞
    }

    // 查询某人对某实体的点赞状态，某人是否给某实体点过赞；  1-点过、0-没有、以后可以扩展 -1 -踩过之类
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        // isMember() 方法代表 userId 是否是 entityLikeKey 这个 set 集合里的元素之一
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        // 该key用于统计某个用户的获得的所有赞，该key以userId作唯一标识；like:user:userId -> int
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

}
