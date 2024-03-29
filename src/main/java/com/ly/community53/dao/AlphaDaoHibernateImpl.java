package com.ly.community53.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

/**
 * @author malaka
 * @create 2020-12-24 13:42
 */
@Repository("alphaHibernate") // 告诉Spring，让Spring创建一个名字叫"alphaHibernate"的GoodsServiceImpl实例。
//@Primary
public class AlphaDaoHibernateImpl implements AlphaDao {
    @Override
    public String select() {
        return "hibernate";
    }
}
