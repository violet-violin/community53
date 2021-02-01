package com.ly.community53.controller;

import com.ly.community53.entity.DiscussPost;
import com.ly.community53.entity.Page;
import com.ly.community53.service.ElasticsearchService;
import com.ly.community53.service.LikeService;
import com.ly.community53.service.UserService;
import com.ly.community53.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        // es中搜索帖子；  page.getCurrent() - 1  ==> 自定义的Page是从current==1开始，故要 - 1
        Map<Long, List<DiscussPost>> searchResult = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        Set<Long> longs = searchResult.keySet();
        Iterator<Long> iterator = longs.iterator();
        Long next = iterator.next();
        


        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult.get(next)) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 帖子的点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        // 分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 : next.intValue());//检索出的帖子总数

        return "/site/search";
    }

}
