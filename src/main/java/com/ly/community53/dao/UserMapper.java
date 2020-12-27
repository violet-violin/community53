package com.ly.community53.dao;

import com.ly.community53.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @author malaka
 * @create 2020-12-27 11:35
 */
//@Repository      //我加@Mapper的话，  @Autowired private UserMapper userMapper;  就报错。加@Repository  才不报错
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
