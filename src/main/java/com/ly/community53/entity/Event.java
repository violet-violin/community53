package com.ly.community53.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author malaka
 * @create 2021-01-11 14:37
 */
public class Event {

    private String topic;//主题，就是事件的类型
    private int userId;//这个事件的触发人
    private int entityType;// 事件（点赞、评论等）发生在哪个实体上，entityType代表实体类型（帖子、评论）
    private int entityId;   //实体的id，如帖子id,评论id
    private int entityUserId;  //这个实体的作者
    private Map<String, Object> data = new HashMap<>();  //后续处理其他事件时，存特殊数据；现在还不知道到底用什么类型来封装，就用map

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {//set方法，加个返回值this；就可以用set(“test”).set()....
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {//外界调用时，传k-v
        this.data.put(key, value);
        return this;
    }

}