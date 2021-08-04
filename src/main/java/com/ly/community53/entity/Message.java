package com.ly.community53.entity;

import java.util.Date;

/**
 * @author malaka
 * @create 2021-01-06 9:03
 */
public class Message {

    private int id;
    private int fromId;//    --from_id = 1，是系统通知
    private int toId;
    //如111_112；还有from_id为1的有3种：comment、like、follow；这个字段方便查询该表  111_112(111 -> 112 ; 112 -> 111)都是一个会话内
    private String conversationId;
    private String content;
    private int status;   // COMMENT '0-未读;1-已读;2-删除;'
    private Date createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
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
        return "Message{" +
                "id=" + id +
                ", fromId=" + fromId +
                ", toId=" + toId +
                ", conversationId='" + conversationId + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}
