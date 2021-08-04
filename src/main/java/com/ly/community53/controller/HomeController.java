package com.ly.community53.controller;

import com.ly.community53.entity.DiscussPost;
import com.ly.community53.entity.Page;
import com.ly.community53.entity.User;
import com.ly.community53.service.DiscussPostService;
import com.ly.community53.service.LikeService;
import com.ly.community53.service.UserService;
import com.ly.community53.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author malaka
 * @create 2020-12-27 16:00
 */
@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/",method = RequestMethod.GET)
    public String root(){
        return "forward:/index";   // 请求转发
    }

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name="orderMode", defaultValue = "0") int orderMode) {
        // 点击下一页时，current+1，然后这个current会自动赋值给形参里的page对象； current 是page对象的属性，current就影响了page.getOffset
        // 其他page没有的参数就是null，可以方法里面再赋值
        // 方法调用前,SpringMVC会自动实例化Model和Page,并将Page注入Model.
        // why？？page的current属性信息什么时候从页面封装入page对象的？前端传入时自动封装 √√√√
        // 实话，这里分页怎么做的？可以再看看视频，每次只从数据库去page.limit条的数据出来
        // 所以,在thymeleaf中可以直接访问Page对象中的数据.
        page.setRows(discussPostService.findDiscussPostRows(0)); // 查询到所有帖子总数
        page.setPath("/index?orderMode=" + orderMode);   //path是   查询路径(用于复用分页链接)


        List<DiscussPost> list = discussPostService.
                findDiscussPosts(0, page.getOffset(), page.getLimit(),orderMode);//这是从数据库取的数据,所有的帖子集合；热门贴就是从本地缓存中取
        List<Map<String, Object>> discussPosts = new ArrayList<>();//这里存储给页面的数据
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());//帖子发布人
                map.put("user", user);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }

    // 该方法用于 统一异常处理（500的异常）的 重定向
    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }



    // 拒绝访问时的提示页面————权限不足的情况；见SecurityConfig 权限不足时会调用该方法到达404页面
    @RequestMapping(path = "/denied", method = {RequestMethod.GET})
    public String getDeniedPage() {
        return "/error/404";
    }
}
