package com.ly.community53.controller;

import com.ly.community53.entity.Event;
import com.ly.community53.entity.Page;
import com.ly.community53.entity.User;
import com.ly.community53.event.EventProducer;
import com.ly.community53.service.FollowService;
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

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;


    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        //todo 该功能应登录后使用，使用拦截器实现
        //拦截器实现—（implements HandlerInterceptor、implements WebMvcConfigurer做配置）；
        //像下面这样if判断行吗？
//        if (user == null) {
//            throw new RuntimeException("该用户不存在!");//或是重定向，return "redirect:/login";
//        }

        //就演示下如何关注一个人，其他的关注自己做
        followService.follow(user.getId(), entityType, entityId);

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);//关注事件，没存postId，它不像赞、评论一样，有作用的帖子
        eventProducer.fireEvent(event);//生产者，事件发布


        return CommunityUtil.getJSONString(0, "已关注!");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        //todo 该功能应登录后使用，使用拦截器实现


        followService.unfollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注!");
    }


    //查询某用户关注的人(从offset到limit的索引范围；如【0，-1】等)
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");  //这里的u,全是userId关注的人
                map.put("hasFollowed", hasFollowed(u.getId()));  //这里岂非全是true
            }
        }
        model.addAttribute("users", userList);

        return "/site/followee";
    }

    //查询某用户的粉丝详情
    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");//这里的u, 全是userId的粉丝
                map.put("hasFollowed", hasFollowed(u.getId()));//反映我是否关注了我的粉丝
            }
        }
        model.addAttribute("users", userList);

        return "/site/follower";
    }

    //是否关注某人（userId）
    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }

        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}
