package com.ly.community53.controller.interceptor;

import com.ly.community53.entity.LoginTicket;
import com.ly.community53.entity.User;
import com.ly.community53.service.UserService;
import com.ly.community53.util.CookieUtil;
import com.ly.community53.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * @author malaka
 * @create 2020-12-30 15:55
 */
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    // 在Controller之前执行；该方法若返回false，则终止执行后续的请求。每访问一个url，就对应一个controller方法，就拦截该controller方法。
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取凭证
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            // 查询凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket);  //直接从redis中取
            // 检查凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                // 根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());  // 这是从redis里面查找用户信息。
                // 在本次请求中持有用户
                hostHolder.setUser(user);


                //  这一部分代码 ———— 干啥的？？SecurityContextHolder是存入的用户认证的结果，我觉得就是存入当前登录用户的权限
                // 构建用户认证的结果(一个Token),并存入SecurityContext,以便于Security进行授权.
                // SecurityContextHolder(我估计也是一个域对象)，SecurityContextHolder是存入的用户认证的结果；HostHolder存入的是用户
                // 存入认证结果  ————  principal: 主要信息; credentials: 证书（如密码或代替密码的信息）; authorities: 权限;
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    // 在Controller之后执行 ；ModelAndView 在controller1之后，再处理可能有用。如对页面就有用，可以${loginUser.username}...
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);//往mav里存入user
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();//在模板都渲染完成后，清理存入的user；否则threadLocal 会有内存泄露的风险
        SecurityContextHolder.clearContext();//清除SecurityContextHolder的内容(SecurityContextHolder是存入的用户认证的结果)
    }
}
