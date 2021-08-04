package com.ly.community53.dao;

import com.ly.community53.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.stereotype.Repository;

/**
 * @author malaka
 * @create 2020-12-27 11:35
 */
// @Mapper 一定要有，否则 Mybatis 找不到 mapper。
// @Repository 可有可无，可以消去依赖注入的报错信息。
// @MapperScan 可以替代 @Mapper。
// @Mapper注解是mybatis的注解，是用来说明这个是一个Mapper，对应的xxxMapper.xml就是来实现这个Mapper。
// 然后再server层使用@Autowired注解引用进来，autowired引入时可能会报红，但不影响使用；加入@Repository 可以去掉报红
@Repository      //我加@Mapper的话，  @Autowired private UserMapper userMapper;  就报错。加@Repository  才不报错
@Mapper   //@Repository 也可   //还是要@Mapper才能通过测试，@Repository  汇报no such bean   ——> UserMapper
public interface UserMapper {
    User selectById(int id);

    User selectByName(String username);

    User selectByEmail(String email);

    int insertUser(User user);

    int updateStatus(int id, int status);

    int updateHeader(int id, String headerUrl);

    int updatePassword(int id, String password);
}
