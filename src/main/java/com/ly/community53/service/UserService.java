package com.ly.community53.service;

import com.ly.community53.dao.UserMapper;
import com.ly.community53.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author malaka
 * @create 2020-12-27 15:50
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

}
