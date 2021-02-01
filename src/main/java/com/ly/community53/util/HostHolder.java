package com.ly.community53.util;

import org.springframework.stereotype.Component;

import org.springframework.stereotype.Component;
import com.ly.community53.entity.User;

/**
 * 起容器的作用，持有用户信息,用于代替session对象
 * @author malaka
 * @create 2020-12-30 16:08
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user) {
        users.set(user);//存到ThreadLocal里，和线程相关。
    }

    public User getUser() {
        return users.get();
    }

    public void clear() {
        users.remove();
    }

}