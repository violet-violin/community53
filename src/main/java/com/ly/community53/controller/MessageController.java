package com.ly.community53.controller;

import com.alibaba.fastjson.JSONObject;
import com.ly.community53.entity.Message;
import com.ly.community53.entity.Page;
import com.ly.community53.entity.User;
import com.ly.community53.service.MessageService;
import com.ly.community53.service.UserService;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.CommunityUtil;
import com.ly.community53.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

/**
 * @author malaka
 * @create 2021-01-06 9:59
 */
@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    // 私信列表——查询当前用户的会话列表,针对每个会话只返回一条最新的私信
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
//        Integer.valueOf("abc");  //故意写的错误语句，为了到达500.html

        User user = hostHolder.getUser();
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        // 会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                //存入私信
                map.put("conversation", message);
                //私信数量
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                //未读私信数量
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));

                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        // 查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        //所有未读通知（包括comment、like、follow三个主题）
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);

        // 私信目标用户
        model.addAttribute("target", getLetterTarget(conversationId));

        // 设置已读
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    //返回私信的目标用户
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);

        if (hostHolder.getUser().getId() == id0) {
            return userService.findUserById(id1);
        } else {
            return userService.findUserById(id0);
        }
    }

    //得到集合中未读消息的id
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
//        Integer.valueOf("abc");//故意写错的语句，为了500.html测试
        //得到对话目标
        User target = userService.findUserByName(toName);

        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }

        //设置私信的属性：from_id、to_id、conversation_id、content、createTime，并插入数据库
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        //todo 将来统一处理异常

        //若没报错，给页面返回状态——0
        return CommunityUtil.getJSONString(0);
    }


    //查询系统通知列表
    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();

        // 查询评论类通知；一旦有人给你评论，就会触发发帖事件；系统会给你发一条通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        Map<String, Object> messageVO = new HashMap<>(); //用来封装评论类通知
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());//反转 content 的转义字符
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);//message的String content字段转map

            // 对于评论的系统通知message来说 user、entityType、entityId、postId这四个字段，
            // 都放入了message的  Map<String, Object> content字段。（之前从json字符串转过来的）
            // message.getContent() 对于系统通知：message的content字段如下：{"entityType":2,"entityId":60,"postId":249,"userId":134}
            // 这些都是even的字段转为json存入message的content字段:{"entityType":2,"entityId":60,"postId":249,"userId":134}
            // 把一条系统通知需要用到的信息都转为json字符串 存入了message的content字段，具体见EventConsumer对于评论、点赞、关注事件的处理
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);// 评论类通知总数
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);// 评论类未读通知总数
            messageVO.put("unread", unread);
        }else{
            messageVO.put("message", null);  //视频时后期发现的bug，把n=messageVO的new、model.add...放入if中，改下notice页面为${}
        }
        model.addAttribute("commentNotice", messageVO);

        // 查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);
            // message.getContent() 对于系统通知：message的content字段如下：{"entityType":2,"entityId":60,"postId":249,"userId":134}
            // 把一条系统通知需要用到的信息都转为json字符串 存入了message的content字段，具体见EventConsumer对于评论、点赞、关注事件的处理
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);//message的String content字段转map

            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unread", unread);
        }else{
            messageVO.put("message", null);
        }
        model.addAttribute("likeNotice", messageVO);

        // 查询关注类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);
            // message.getContent() 对于系统通知：message的content字段如下：{"entityType":2,"entityId":60,"postId":249,"userId":134}
            // 这些都是even的字段转为json存入message的content字段:{"entityType":2,"entityId":60,"postId":249,"userId":134}
            // 把一条系统通知需要用到的信息都转为json字符串 存入了message的content字段，具体见EventConsumer对于评论、点赞、关注事件的处理
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread", unread);
        }else{
            messageVO.put("message", null);
        }
        model.addAttribute("followNotice", messageVO);

        // 查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);//所有未读私信
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        //所有未读通知（包括comment、like、follow三个主题）
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }


    //该方法用于点击进入系统通知（点赞、评论、关注三者之一）列表
    //{topic}从页面传来，topic从notice.html传来，再进入notice-detail.html
    //上面getNoticeList方法的model里面有message，message表有conversation_id字段，就是topic
    //woc，视频直接再notice页面写死了comment  th:href="@{/notice/detail/comment}"  (没毛病，notice.html页面就者3条)
    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)//url的topic从页面哪儿来？？
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        User user = hostHolder.getUser();//当前登录用户
        // 点击下一页时，current+1，然后这个current会自动赋值给形参里的page对象； current 是page对象的属性，current就影响了page.getOffset
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic)); // 查询出 评论总数，用于分页

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());  //反转义 html
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);//message的String content字段转map
                // 对于评论、点赞、关注的系统通知message来说 user、entityType、entityId、postId这四个字段，
                // 都放入了message的  Map<String, Object> content字段。（之前从json字符串转过来的）
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId")); //关注的controller，没有存入“postId”，就是null
                // 通知作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(noticeList);//得到这一页的集合中未读消息的id
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
