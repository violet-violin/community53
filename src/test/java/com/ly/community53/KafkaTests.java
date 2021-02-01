package com.ly.community53;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test", "你好、、、、、、、、、、、、、、、、、、、、、、、、、、、、、");
        kafkaProducer.sendMessage("test", "在吗、、、、、、、、、、、、、、、、、、、、");
        kafkaProducer.sendMessage("test", "is there？？？？？？？？？？？？？？？？？？？？？");

        try {
            Thread.sleep(1000 * 10);//主线程睡眠10s结束程序，这段时间用于等待消费者消费
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

@Component  //把Kafka生产者用容器来管理
class KafkaProducer {//生产者，生产消息时主动的

    @Autowired
    private KafkaTemplate kafkaTemplate;  //KafkaTemplate  该类自动被容器整合了

    public void sendMessage(String topic, String content) {
        kafkaTemplate.send(topic, content);
    }

}

@Component  //用容器来管理
class KafkaConsumer {//消费者，消费消息是被动的

    @KafkaListener(topics = {"test"})  //test主题
    public void handleMessage(ConsumerRecord record) {//把消息封装为ConsumerRecord
        System.out.println(record.value());//打印，读出record封装的消息
    }
}