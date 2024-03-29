package com.ly.community53.controller;

import com.google.code.kaptcha.Producer;
import com.ly.community53.entity.User;
import com.ly.community53.service.UserService;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.CommunityUtil;
import com.ly.community53.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private Producer kaptchaProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")//从application.properties注入contextPath属性，用于设置cookie在项目整体目录下有效
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> map = userService.register(user,request,response);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {//map中有值，注册失败；需要处理
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    // http://localhost:8080/community53/activation/101/code       //@PathVariable————从路径中取值
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");//target是前端页面在用；激活成功就去登录页面
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");//激活未成功就去首页
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    //生成验证码图片：http://localhost:8080/community53/kaptcha
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码  text
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);//用验证码生成图片

        // 将验证码存入session
//        session.setAttribute("kaptcha", text);

        // 验证码的归属 ——> kaptchaOwner 什么意思 ？
        // 这里生成"kaptchaOwner"的cookie，是为了后续登陆login()方法从cookie里取出kaptchaOwner并生成对应redis的key，并找到服务端保存的验证码
        String kaptchaOwner = CommunityUtil.generateUUID(); // 生成随机字符串
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);//这里生成"kaptchaOwner"的cookie，
        cookie.setMaxAge(60);  //60秒
        cookie.setPath(contextPath);//cookie生效范围
        response.addCookie(cookie);
        // 将验证码存入Redis  （redisKey, text）
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);


        // 将图片输出给浏览器
        response.setContentType("image/png");//声明给浏览器什么格式的数据
        try {
            OutputStream os = response.getOutputStream();//这个os由response管理，不用自己关闭
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    //根据请求方法不同，可以区别两个  /login 路径
    //该方法用于用户登录功能
    //形参中有username、password等参数，spring不会把这些基本参数放入model里；如果是User等bean，spring会自动把该形参放入model，前端页面直接用就行。
    //这些username、password、code(前端输过来的验证码)等参数 存在于request对象里，从请求里带过来的，login.html里的 提交的表单的name属性与形参  一致。
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, /*HttpSession session, */HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {// 从cookie中取值————kaptchaOwner， LoginController#getKaptcha()方法new的cookie。
        // 首先检查验证码——code
//        String kaptcha = (String) session.getAttribute("kaptcha");//上面的getKaptcha()把验证码text存入了session
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner); //生成一个字符串：kaptcha: + owner
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }                                    // code是前端提交过来的验证码, kaptcha是redis里保存60s的验证码
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";   //验证码不对就返回登录页面
        }

        // 检查账号,密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;//超时时间
        Map<String, Object> map = userService.login(username, password, expiredSeconds);  //map 里面存了登录凭证，
        if (map.containsKey("ticket")) {  // 说明登录时  是在redis成功生成了凭证
            //服务端new cookie，就是用凭证的随机字符串进行new cookie; 以后的请求都会带上这个cookie
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);//cookie在项目整体目录下有效
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);  //把cookie传给浏览器
            return "redirect:/index";    //这是重定向  进入首页  写成return “/index”  行吗？？  请求转发 forward，地址栏不会变
        } else {//map没有ticket，就返回登录页面，继续重新登录
            //html用到：<div class="invalid-feedback" th:text="${usernameMsg}">该账号不存在!</div>
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

// 请求转发，即request.getRequestDispatcher().forward()，是一种服务器的行为，客户端只有一次请求，
//服务器端转发后会将请求对象保存，地址栏中的URL地址不会改变，得到响应后服务器端再将响应发给客户端；同一个请求有两个方法来处理。

// 请求重定向：请求重定向，即response.sendRedirect()，是一种客户端行文，从本质上讲等同于两次请求，前一次请求对象不会保存，地址栏的URL地址会改变。


    //用户退出；@CookieValue——取cookie中的数据，"ticket"的cookie是在login方法里new出来的
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        //退出时，也要清理SecurityContextHolder的内容
        // 见LoginTicketInterceptor#postHandle()方法，SecurityContextHolder是存入的用户认证的结果
        SecurityContextHolder.clearContext();
        return "redirect:/login";          //重定向时，默认是get请求，故是get的/login
    }

}
