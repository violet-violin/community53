package com.ly.community53.service;

import com.ly.community53.dao.LoginTicketMapper;
import com.ly.community53.dao.UserMapper;
import com.ly.community53.entity.LoginTicket;
import com.ly.community53.entity.User;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.CommunityUtil;
import com.ly.community53.util.MailClient;
import com.ly.community53.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.context.webmvc.SpringWebMvcThymeleafRequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private RedisTemplate redisTemplate;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Value("${community53.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id) {
//        return userMapper.selectById(id);
        User user = getCache(id); // 1.优先从redis缓存中取值
        if (user == null) {
            user = initCache(id); // 2.取不到时初始化缓存数据(从数据库中取，并缓存入redis)
        }
        return user;
    }


    public Map<String, Object> register(User user, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号名字 是否已存在
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;   //return之后，就不再往下走了
        }

        // 验证邮箱  是否已被注册
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户——盐值、密码、用户类型、激活状态、激活码
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());

        //设置头像的url
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 激活邮件
//        Context context = new Context();

        //要使用相对定位，必须指定一个实现IWebcontext接口的对象，
        // IWebcontext对象可以传入request,response,servletContext参数，可以用来定位应用程序的根路径
        // 这样就可以发邮件带  静态图片了
        WebContext context = new WebContext(request,response,request.getServletContext());
//        context.setVariable("imgPath","D:"+ File.separator+"test"+File.separator+"0017031042011883_b.jpg");
//        context.setVariable("logo", "这是一张图片");


        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community53/activation/101(用户id)/code(激活码);  把用户id/激活码 变成一个 url 给到 要发送到html页面
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url); // 这个context是作为域对象使用，存储数据；供前端html页面取值

        //thymeleaf的   要发的  激活邮件模板——mail/activation.html
        String content = templateEngine.process("/mail/activation", context);  //把context的内容给激活邮件的html模板，再把模板给content，下一句发邮件时要用
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;//若map是空的，则注册成功
        
    }

    //激活账号
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {  //User status : 用户是否已激活 0-未激活；1-已激活
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {//验证激活码无误，就进入数据库激活
            userMapper.updateStatus(userId, 1);
            //每次修改user的状态都要清缓存；因为此时用户的状态变味了已激活，故缓存数据需要删除；
            // 后面再用到的时候会先从db查出最新数据，再放入redis缓存
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    //用户登录的业务逻辑
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态，可能账号未激活
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 生成登录凭证 new LoginTicket()    //账号、密码都验证通过了，可以生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());  //ticket是随机生成的字符串
        loginTicket.setStatus(0);  // status ： 0 / 1  0-登录凭证有效、1-登录凭证失效
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);//把登录凭证插入数据库；login_ticket表就类似于session，不过该表是放在数据库的

        //使用redis存登录凭证，而非用上面注掉的插入数据库；登录凭证朝生夕死的，确实不适合一直存数据库
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());  // 该方法生成redis用到的key ——>  ticket: + 上面的随机字符串
        redisTemplate.opsForValue().set(redisKey, loginTicket);//redis会把loginTicket序列化为json字符串
        // 把用户的token为键和用户的LoginTicket为值保存在redis中，每次请求，浏览器都在cookie中带上这个token,
        // 登陆时去redis找，判断是否存在对应的LoginTicket对象，并且判断是否过期，如果存在且没有过期才去数据库中查找用户，
        // 把用户对象放到ThreadLocal中，方便业务使用。

        map.put("ticket", loginTicket.getTicket());
        return map;//map会传给controller层login()方法，map存的值在controller方法传给model，在html会页面用到
    }

    //用户退出，根据凭证来退出；0-有效、1-失效
    // 这些数据不要删，以后做用户每年登录多少次、多久注册这些功能；这些数据有用
    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket); //修改loginTicket对象的status状态为1，并重新设置回redis
    }

    //根据ticket查 LoginTicket
    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);  //直接从redis中取
    }

    //更新用户头像的url，即headerUrl
    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);  //redis、数据库的事务是分开的；就不能把更新数据库、更新redis放在一个事务
        return rows;
    }
    //更新用户密码，即User.password
    public int updatePassword(int userId, String password){
        int rows = userMapper.updatePassword(userId, password);
        clearCache(userId);
        return rows;
    }

    public User findUserByName(String userName) {
        return userMapper.selectByName(userName);
    }


    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);//从redis中取，根据userId生成的redisKey取user对象
    }

    // 2.取不到时初始化缓存数据(从数据库中取，并缓存入redis)
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);//过期时间3600秒，1h
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    //  做啥呢？———— 根据用户id，返回用户相应的权限 //用户类型，即User对象的属性：1-管理员；2-版主；默认0-普通用户
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()) { //  `type` int(11) DEFAULT NULL COMMENT '0-普通用户; 1-超级管理员; 2-版主;',
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
