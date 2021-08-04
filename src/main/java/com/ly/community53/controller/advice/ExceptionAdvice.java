package com.ly.community53.controller.advice;

import com.ly.community53.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author malaka
 * @create 2021-01-06 17:12
 */
@ControllerAdvice(annotations = Controller.class)  //只对controller注解的bean进行扫描处理
//    - 用于修饰类，表示该类是Controller的全局配置类。
//    - 在此类中，可以对全局各个Controller进行如下三种全局配置：
//    异常处理方案（@ExceptionHandler）、绑定数据方案（@ModelAttribute）、绑定参数方案（@DataBinder）
//    我们这里主要就是用  @ControllerAdvice + @ExceptionHandler
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})//用于修饰方法，该方法会在全局的各个Controller出现异常后被调用，用于**处理捕获到的异常**。
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: " + e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) { // 遍历异常的 栈的信息
            logger.error(element.toString());
        }

        String xRequestedWith = request.getHeader("x-requested-with");
        //如果请求头是 "XMLHttpRequest"，XMLHttpRequest对象是 AJAX 的主要接口，用于浏览器与服务器之间的通信
        //  就是说 这是异步请求，就返回 json数据（只有异步请求才期待返回一个xml/json）；同步请求就返回 error页面
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8"); // 也可以写成"application/json;charset=utf-8"；plain代表普通的字符串
            PrintWriter writer = response.getWriter();
            // 异步请求出错会返回这个json字符串；这个write()方法将json字符串写到了哪里？浏览器f12?
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error"); //请求头不是 "XMLHttpRequest"，就直接重定向到error页面
        }
    }

}
