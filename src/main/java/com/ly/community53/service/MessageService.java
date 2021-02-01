package com.ly.community53.service;

import com.ly.community53.dao.MessageMapper;
import com.ly.community53.entity.Message;
import com.ly.community53.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * @author malaka
 * @create 2021-01-06 9:55
 */
@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));//转义，过滤标签
        message.setContent(sensitiveFilter.filter(message.getContent()));//过滤敏感词
        return messageMapper.insertMessage(message);
    }
    //阅读消息，改变消息状态即可
    public int readMessage(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }


    //查找最新系统通知
    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }
    //查找系统通知数量：topic传null时不区分主题，查所有
    public int findNoticeCount(int userId, String topic) {
        return messageMapper.selectNoticeCount(userId, topic);
    }
    //查找系统未读通知数量：topic传null时不区分主题，查所有
    public int findNoticeUnreadCount(int userId, String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }
    // 查询某个主题所包含的通知列表；   分页记得要用到  查出的总数
    public List<Message> findNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }

}
