package com.ly.community53;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings() {
        String redisKey = "test:count";

        redisTemplate.opsForValue().set(redisKey, 1);

        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashes() {
        String redisKey = "test:user";

        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");

        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));//1
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));//zhangsan
    }

    @Test
    public void testLists() {
        String redisKey = "test:ids";

        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);

        System.out.println(redisTemplate.opsForList().size(redisKey));//3
        System.out.println(redisTemplate.opsForList().index(redisKey, 0));//103
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));//[103 102 101]

        System.out.println(redisTemplate.opsForList().leftPop(redisKey));//103
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));//102
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));//101
    }

    @Test
    public void testSets() {
        String redisKey = "test:teachers";

        redisTemplate.opsForSet().add(redisKey, "刘备", "关羽", "张飞", "赵云", "诸葛亮");

        System.out.println(redisTemplate.opsForSet().size(redisKey)); // 5
        System.out.println(redisTemplate.opsForSet().pop(redisKey));//随机弹出一个 张飞
        System.out.println(redisTemplate.opsForSet().members(redisKey)); // 返回一个Set<V>  [关羽, 刘备, 赵云, 诸葛亮]
    }

    @Test
    public void testSortedSets() {
        String redisKey = "test:students";

        redisTemplate.opsForZSet().add(redisKey, "唐僧", 80);
        redisTemplate.opsForZSet().add(redisKey, "悟空", 90);
        redisTemplate.opsForZSet().add(redisKey, "八戒", 50);
        redisTemplate.opsForZSet().add(redisKey, "沙僧", 70);
        redisTemplate.opsForZSet().add(redisKey, "白龙马", 60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));//个数  5
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "八戒"));  // 50.0
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "八戒"));//由大到小取排名：4
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "悟空"));//由大到小取排名：0
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));//由大到小的范围取值前三名：[悟空, 唐僧, 沙僧]
    }

    //常用命令
    @Test
    public void testKeys() {
        redisTemplate.delete("test:user");

        System.out.println(redisTemplate.hasKey("test:user")); // false

        // Set time to live for given {@code key}.
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    // 多次访问一个key，批量发送命令,节约网络开销。  批量发送命令就是用 boundValueOps()方法来绑定。
    @Test
    public void testBoundOperations() {
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);//提前绑定这个key
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());  // 5
    }

    // 编程式事务：redis的事务管理较简单；redis中声明式事务用的少，不演示了。
    @Test
    public void testTransaction() {
        Object result = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String redisKey = "text:tx";

                // 启用事务
                redisOperations.multi();
                redisOperations.opsForSet().add(redisKey, "zhangsan");
                redisOperations.opsForSet().add(redisKey, "lisi");
                redisOperations.opsForSet().add(redisKey, "wangwu");//开启事务后，会把这些命令放入队列，暂不执行

                System.out.println(redisOperations.opsForSet().members(redisKey));//[]

                // 提交事务
                return redisOperations.exec();
            }
        });
        System.out.println(result);//[1, 1, 1, [wangwu, lisi, zhangsan]]
    }



    // 统计20万个重复数据的独立总数.
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";

        for (int i = 1; i <= 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }//存储[1, 100000]

        for (int i = 1; i <= 100000; i++) {
            int r = (int) (Math.random() * 100000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }//存储[1,100000]的随机数

        long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);//99553 (统计的是两次添加后redisKey里该有的数据的量)；标准值为100000，但统计有误差，标准误差为0.81%；
    }

    // 将3组数据合并, 再统计合并后的重复数据的独立总数.
    @Test
    public void testHyperLogLogUnion() {
        String redisKey2 = "test:hll:02";
        for (int i = 1; i <= 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }

        String redisKey3 = "test:hll:03";
        for (int i = 5001; i <= 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }

        String redisKey4 = "test:hll:04";
        for (int i = 10001; i <= 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }

        String unionKey = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);

        long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);//19833；标准值20000，标准误差为0.81%；统计的是unionKey该有的数据的量   基数算法？？原理？？
    }

    // 统计一组数据的布尔值
    @Test
    public void testBitMap() {
        String redisKey = "test:bm:01";

        // 记录；BitMap还是String，支持按位存取数据，就是对String的特殊操作；
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);

        // 查询某位布尔值情况
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));//false
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));//true
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));//false

        // 统计1的个数
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });

        System.out.println(obj);//3
    }

    // 统计3组数据的布尔值, 并对这3组数据做OR运算. 也可做与运算
    @Test
    public void testBitMapOperation() {
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);

        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);

        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);
//        redisTemplate.opsForValue().setBit(redisKey4, 10, true);

        String redisKey = "test:bm:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
                return connection.bitCount(redisKey.getBytes());
            }
        });

        System.out.println(obj);//7   0--6

        // 0-6 全是true
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 5));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 6));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 7));  // false
//        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 9));
//        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 10));
    }

}
