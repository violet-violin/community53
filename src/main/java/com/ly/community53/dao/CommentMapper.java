package com.ly.community53.dao;

import com.ly.community53.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author malaka
 * @create 2021-01-05 16:12
 */
@Repository
@Mapper
public interface CommentMapper {

    //查出所有的评论(对帖子的评论entityType=1、对评论的评论entityType=2)；
    //entityType=1 时，entityId为帖子id，entityType=2时，entityId为评论id
    // 支持对评论的分页，就加了offset、limit
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    //查询帖子的评论总数
    int selectCountByEntity(int entityType, int entityId);

    //添加评论
    int insertComment(Comment comment);


    //根据user_id查询评论//要不要分为对帖子的评论、对评论的评论呢？  暂定不需要；后续要的话就加形参
    List<Comment> selectCommentsByUserId(int userId, int offset, int limit);

    //查询某user_id发了多少条评论
    int selectCountByUserId(int userId);

    //根据id找到entity_id：用于找到评论的评论 的帖子是谁
    int selectEntityIdById(int id);


    //根据id查评论
    Comment selectCommentById(int id);
}
