package com.ly.community53.dao;

import com.ly.community53.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author malaka
 * @create 2021-01-06 9:05
 */
@Mapper
public interface MessageMapper {

    // 查询当前用户的会话列表,针对每个会话只返回一条最新的私信.  支持分页
    List<Message> selectConversations(int userId, int offset, int limit);

    // 查询当前用户的总会话数量.
    int selectConversationCount(int userId);

    // 查询某个会话所包含的私信列表.  支持分页
    List<Message> selectLetters(String conversationId, int offset, int limit);

    // 查询某个会话所包含的私信数量.
    int selectLetterCount(String conversationId);

    // 查询未读私信的数量；  传个conversationId来干嘛？（也可传null）
    int selectLetterUnreadCount(int userId, String conversationId);

    // 新增消息
    int insertMessage(Message message);

    // 修改消息的状态：未读消息变为已读、设置删除也可
    int updateStatus(List<Integer> ids, int status);


    // 查询某个主题下最新的通知
    Message selectLatestNotice(int userId, String topic);

    // 查询某个主题所包含的通知数量
    int selectNoticeCount(int userId, String topic);

    // 查询未读的通知的数量
    int selectNoticeUnreadCount(int userId, String topic);

    // 查询某个主题所包含的通知列表；   分页记得要用到  查出的总数
    List<Message> selectNotices(int userId, String topic, int offset, int limit);

}
