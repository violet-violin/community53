package com.ly.community53.dao;

import com.ly.community53.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

/**
 * @author malaka
 * @create 2020-12-29 14:44
 */
@Mapper
@Deprecated
public interface LoginTicketMapper {//这个mapper用注解来写mybatis的实现；也可以用xml

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",//每句话后记得加空格
            "values(#{userId},#{ticket},#{status},#{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")//自动生成主键，并把自动生成的值注入LoginTicket类的id属性
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);//查出五个字段后，会自动封装入LoginTicket

    @Update({
            "<script>",  //动态sql ——> 用<script>脚本
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",    //  \"  转义  “
            "and 1=1 ",        //ticket!=null时，就加上 and 1=1  //虽然恒成立
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);

}
