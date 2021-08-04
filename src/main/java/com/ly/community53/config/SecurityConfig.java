package com.ly.community53.config;

import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override// 忽略静态资源的访问，security使用filter————把静态资源的访问给忽略了
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override  //////授权的方法:user、admin、moderator  （包含登录、退出、授权、验证码处理、记住我等功能）
    protected void configure(HttpSecurity http) throws Exception {
        // 授权，这些路径要————user、admin、moderator
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/user/updatePassword",
                        "/discuss/add",
                        "/discuss/myPosts",
                        "/comment/add/**",
                        "/comment/myComments",   
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow",
                        "/followees/**",
                        "/followers/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll()
                .and().csrf().disable();//不启用防止csrf攻击功能;
        // 想做的话就每个异步请求功能挨个来，就是因为麻烦，视频没做csrf检查，只举了个例子
        // 对于每一个同步请求post，就会自动发一个token到表单，每次提交表单都会带上cookie、token；防止csrf攻击；
        // 对于每一个异步请求post，都要如上面这样处理，处理完成后，每次异步请求也会带上这个cookie、token，防止csrf攻击；

        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 没有登录时的处理
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        //请求有同步、异步之分，通过请求头"x-requested-with"  来区分
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {//代表异步请求，异步请求会返回json
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦!"));
                        } else {//同步请求，就是重定向了；没登陆就去/login路径
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 权限不足时的处理，也有同、异步请求之分
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) { // 异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        } else {
                            //HomeController中追加 /denied路径的处理方法
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });

        // Security底层默认会拦截/logout请求,进行退出处理.
        // 覆盖它默认的逻辑（即拦截/logout）,才能执行我们自己的退出代码.
        //把默认的/login 改为 /securitylogout，但其实程序中没有这个路径，filter就拦截不了/logout。
        // 就会执行自己的/logout，见LoginController里的方法logout()
        http.logout().logoutUrl("/securitylogout");
    }

}
