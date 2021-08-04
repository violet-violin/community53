package com.ly.community53.dao;

import com.ly.community53.entity.LoginTicket;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * @author malaka
 * @create 2020-12-29 14:44
 */
@Repository
@Mapper
@Deprecated
public interface LoginTicketMapper {//这个mapper用注解来写mybatis的实现；也可以用xml

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",//每句话后记得加空格
            "values(#{userId},#{ticket},#{status},#{expired})"
    })//其实这句sql应该是写成一句；逗号+分行会被自动拼接成一句；分行是为了好看，但记得每句话后记得加空格如 expired) ",
    @Options(useGeneratedKeys = true, keyProperty = "id")//自动生成主键，并把自动生成的值注入LoginTicket类的id属性
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);//查出五个字段后，会自动封装入LoginTicket

    @Update({
            "<script>",  //这里是演示动态sql ——> 用<script>脚本；其实这句话根本不用 动态sql都行
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",    //  \"  转义  “
            "and 1=1 ",  //ticket!=null，加上and 1=1  //恒成立，这个恒成立的等式在干啥 ——> 仅是为了演示动态sql
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);

}
