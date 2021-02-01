package com.ly.community53.event;

import com.alibaba.fastjson.JSONObject;
import com.ly.community53.entity.DiscussPost;
import com.ly.community53.entity.Event;
import com.ly.community53.entity.Message;
import com.ly.community53.service.DiscussPostService;
import com.ly.community53.service.ElasticsearchService;
import com.ly.community53.service.MessageService;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {//消费者被动触发

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;


    @Value("${wk.image.command}")   //图片存放位置
    private String wkImageCommand;
    @Value("${wk.image.storage}")   //图片存放位置
    private String wkImageStorage;

    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;//线程池调度组件，可以执行定时任务


    //消费评论、点赞、关注事件
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW}) //三个主题？？ 三个主题都要消费
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);//解析json字符串为对象
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        // 发送站内通知，发站内信
        //message字段：id、from_id、to_id、conversation_id content、status、create_time
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());//系统通知，就comment、like、follow三个
        //message的status默认是0？，什么时候定义的？？  数据库 DEFAULT NULL，类型为int(11)，然后就是0
        //mysql int类型默认值设置为空，结果会自动转成0。
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {//看看event的map字段，是否有值，再来处理
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                //把event的map里的数据（这些数据反正不好封装就直接放map了），放入content
                content.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    // 消费发帖事件；把发布的帖子存入es
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

    // 消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }


    // 消费分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd); //这句话比上句话先执行完，就用一个定时任务监视上句exec是否执行完
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        //多个消费者服务器，只有一个能消费该主题，并抢到该方法来执行；故这个定时任务只有一个服务器会执行
        // 启用定时器,监视该图片,一旦生成了,则上传至七牛云.  //定时器监视任务
        UploadTask task = new UploadTask(fileName, suffix);
        //启动定时任务会有一个返回值；根据future的值在run方法中结束定时任务
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);//每500ms执行一次
        task.setFuture(future);
    }

    //线程
    class UploadTask implements Runnable {

        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() { //每半秒执行一次该方法，逻辑就很像递归，开始要检查初始状态。
            // 生成失败
            if (System.currentTimeMillis() - startTime > 30000) {  //程序执行了30s多,执行时间过长
                logger.error("执行时间过长,终止任务:" + fileName);
                future.cancel(true);  //取消执行定时任务
                return;
            }
            // 上传失败
            if (uploadTimes >= 3) { //上传了>=3次
                logger.error("上传次数过多,终止任务:" + fileName);
                future.cancel(true);
                return;
            }

            String path = wkImageStorage + "/" + fileName + suffix; //长图本地存储路径
            File file = new File(path);
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                //开始上传七牛云
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0)); //0，这个0从哪儿装过来的？？
                // 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                //四个参数分别是：存储区域、文件名、凭证过期时间、返回信息
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                // 指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone2()));//zone2代表华南机房
                try {
                    // 开始上传图片
                    Response response = manager.put(//null——代表不写的额外参数；"image/"——上传文件类型
                            path, fileName, uploadToken, null, "image/" + suffix, false);
                    // 处理响应结果，把响应结果转为json
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else { //否则就是上传成功了，并结束定时任务
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }
}
