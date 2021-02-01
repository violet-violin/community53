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
//    - 在此类中，可以对Controller进行如下三种全局配置：异常处理方案、绑定数据方案、绑定参数方案。
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})//用于修饰方法，该方法会在Controller出现异常后被调用，用于**处理捕获到的异常**。
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: " + e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }

        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {//如果请求头是 "XMLHttpRequest"
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }

}
