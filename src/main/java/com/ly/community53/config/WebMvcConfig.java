package com.ly.community53.config;

import com.ly.community53.controller.interceptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author malaka
 * @create 2020-12-30 15:07
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AlphaInterceptor alphaInterceptor;

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;
//    @Autowired
//    private LoginRequiredInterceptor loginRequiredInterceptor;
    @Autowired
    private MessageInterceptor messageInterceptor;
    @Autowired
    private DataInterceptor dataInterceptor;

    //注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //  /**/*.css  /**代表static包下所有文件夹；//  .excludePathPatterns()  ——> 排除对静态资源的拦截
        //我用  /** 报错，用/*才行  ？why？？
        //明确拦截  url路径为/register、/login，并排除静态资源
        //这样，拦截路径下的controller方法、TemplateEngine方法执行前后就会执行对应的拦截器方法，做些处理
        registry.addInterceptor(alphaInterceptor).addPathPatterns("/register", "/login")
                .excludePathPatterns("/*/*.css", "/*/*.js", "/*/*.png", "/*/*.jpg", "/*/*.jpeg");

//        //我用  /** 疯狂报错
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/*/*.css", "/*/*.js", "/*/*.png", "/*/*.jpg", "/*/*.jpeg");

//        registry.addInterceptor(loginRequiredInterceptor)
//                .excludePathPatterns("/*/*.css", "/*/*.js", "/*/*.png", "/*/*.jpg", "/*/*.jpeg");

        //拦截所有动态资源，不拦截所有静态资源
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/*/*.css", "/*/*.js", "/*/*.png", "/*/*.jpg", "/*/*.jpeg");

        //这是做UV、DAU统计的拦截器的配置，拦截所有动态资源；
        //我怎么觉得只用统计UV只要拦截index，   统计DAU只要拦截login页面？ 分成两个拦截器统计？？
        //用一个拦截器也行；就是每次请求都拦截，感觉会不会减低服务器性能??
        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/*/*.css", "/*/*.js", "/*/*.png", "/*/*.jpg", "/*/*.jpeg");
    }

}