package com.ly.community53.controller.interceptor;

import com.ly.community53.annotation.LoginRequired;
import com.ly.community53.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @author malaka
 * @create 2020-12-31 14:52
 */
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    //拦截器实现权限控制，后续被注释掉了，换了security
    // 在Controller之前执行；该方法若若返回false，则终止执行后续的请求。
    //记得把拦截器register，排除静态资源的路径
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {//handler 是拦截目标，需要先判断是否是个方法；如拦截目标是个类，就不用拦截
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if (loginRequired != null && hostHolder.getUser() == null) {//当前方法需要登录，而没有用户，故不能开放此方法，拒绝后续请求。

                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }
        return true;
    }
}
