package com.ly.community53.dao;

import com.ly.community53.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author malaka
 * @create 2020-12-27 15:23
 */
@Repository
@Mapper
public interface DiscussPostMapper {

    //orderMode：默认0-按原来方式排(按照最新，即发帖时间)；1-添加按照热点（即帖子的score属性值）排序
    // userId != 0 就按userId来查， == 0 就是查所有用户发的帖子
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.   为什么必须加 ？ 直接 #{userId} 取出来 不 OK？
    // 查询帖子数量 ——> userId != 0 就看userId用户发帖数量， == 0 就是查所有用户发帖数量
    int selectDiscussPostRows(@Param("userId") int userId);

    //插入帖子的方法
    int insertDiscussPost(DiscussPost discussPost);

    //帖子详情查询
    DiscussPost selectDiscussPostById(int id);

    //修改评论数量；  每次有用户添加评论时都增加一条评论数量，3 charpter 6 小结 添加的该方法
    int updateCommentCount(int id, int commentCount);

    //改变帖子类型，置顶帖子；// COMMENT '0-普通; 1-置顶;',  type 从0到1
    int updateType(int id, int type);

    //改变帖子状态；加精、删除；// COMMENT '0-正常; 1-精华; 2-拉黑;', 从0到1/2；前面查询帖子的方法实现中都有 where status != 2
    int updateStatus(int id, int status);

    //改帖子分数；给帖子点赞、评论、管理员加精、置顶等行为都会改变帖子分数
    int updateScore(int id, double score);


}
