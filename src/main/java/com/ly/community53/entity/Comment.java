package com.ly.community53.entity;

import java.util.Date;

/**
 * @author malaka
 * @create 2021-01-05 16:10
 */
public class Comment {
    private int id;
    private int userId;         // 评论的 作者
    private int entityType;  //  --评论/点赞的目标类型 1-帖子；2-评论；3-课程；之类
    private int entityId;  // --评论/点赞的实体(如帖子/评论)的id;entityType=1 时，entityId为帖子id，entityType=2时，entityId为评论id
    private int targetId;    //--评论指向的人
    private String content;
    private int status;      //-- status == 0，代表评论有效，才可以被查询出来
    private Date createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEntityType() {
        return entityType;
    }

    public void setEntityType(int entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", userId=" + userId +
                ", entityType=" + entityType +
                ", entityId=" + entityId +
                ", targetId=" + targetId +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}
