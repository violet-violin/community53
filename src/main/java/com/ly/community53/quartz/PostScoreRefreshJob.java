package com.ly.community53.quartz;

import com.ly.community53.entity.DiscussPost;
import com.ly.community53.service.DiscussPostService;
import com.ly.community53.service.ElasticsearchService;
import com.ly.community53.service.LikeService;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant { //定时任务，继承Job

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    // 牛客纪元————对应53论坛纪元
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-09-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化53论坛纪元失败!", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {  // 定时任务执行，重写了Job接口里的方法
        String redisKey = RedisKeyUtil.getPostScoreKey();      // redis内存储帖子分数的键
        // BoundSetOperations就是一个绑定key的对象，我们可以通过这个对象来进行与key相关的操作。如add、pop等等
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);  // 该redisKey里面存储的是postId

        if (operations.size() == 0) { //没有点赞、评论这些影响帖子因素的存在，没存入redis
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数: " + operations.size());
        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop());  //refresh()  刷新一个帖子 的分数；pop() 弹出集合中的值
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }


    //刷新一个帖子 的分数 ———— 会更改 MySQL、ES里面的内容
    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if (post == null) {//如有人评论后，管理员删帖；就会出现该情况
            logger.error("该帖子不存在: id = " + postId);
            return;
        }

        // 是否精华
        boolean wonderful = post.getStatus() == 1;
        // 评论数量
        int commentCount = post.getCommentCount();
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        // 计算权重（精华分是75）
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // 分数 = 帖子权重 + 距离天数   score计算公式：log（精华分 + 评论数*10 + 点赞数*2 + 收藏数*2）+（发布时间-牛客纪元）
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        // 更新帖子分数   数据库里discussPost 的变更
        discussPostService.updateScore(postId, score);

        // 同步搜索数据：这也变成定时任务了
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }

}
