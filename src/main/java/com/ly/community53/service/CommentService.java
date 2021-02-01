package com.ly.community53.service;

import com.ly.community53.dao.CommentMapper;
import com.ly.community53.entity.Comment;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * @author malaka
 * @create 2021-01-05 16:20
 */
@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private DiscussPostService discussPostService;

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    //事务方法
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 添加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));//转义html标签
        comment.setContent(sensitiveFilter.filter(comment.getContent()));//敏感词过滤
        int rows = commentMapper.insertComment(comment);

        // 更新帖子的评论数量
        if (comment.getEntityType() == ENTITY_TYPE_POST) {//只有更新帖子时，才更新discuss_post表的comment_count字段
            //先查到帖子的评论数量，这里这个数量已经数据库自动加1了
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());//查到帖子的评论数量
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }

        return rows;
    }

    public List<Comment> findCommentsByUserId(int userId, int offset, int limit) {
        return commentMapper.selectCommentsByUserId(userId, offset, limit);
    }

    public int findCommentCountByUserId(int userId) {
        return commentMapper.selectCountByUserId(userId);
    }

    public int findEntityIdById(int id) {
        return commentMapper.selectEntityIdById(id);
    }

    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }

}
