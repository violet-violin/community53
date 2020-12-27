package com.ly.community53.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

/**
 * @author malaka
 * @create 2020-12-24 13:42
 */
@Repository("alphaHibernate")
//@Primary
public class AlphaDaoHibernateImpl implements AlphaDao {
    @Override
    public String select() {
        return "hibernate";
    }
}
