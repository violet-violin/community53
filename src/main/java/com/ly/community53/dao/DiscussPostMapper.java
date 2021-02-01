package com.ly.community53.dao;

import com.ly.community53.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author malaka
 * @create 2020-12-27 15:23
 */

@Mapper
public interface DiscussPostMapper {

    //orderMode：默认0-按原来方式排(按照最新，即发帖时间)；1-添加按照热点（score）排序
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);

    //插入帖子的方法
    int insertDiscussPost(DiscussPost discussPost);

    //帖子详情查询
    DiscussPost selectDiscussPostById(int id);

    //修改评论数量
    int updateCommentCount(int id, int commentCount);

    //改变帖子类型，置顶帖子
    int updateType(int id, int type);

    //改变帖子状态；加精、删除
    int updateStatus(int id, int status);

    //改帖子分数
    int updateScore(int id, double score);


}
