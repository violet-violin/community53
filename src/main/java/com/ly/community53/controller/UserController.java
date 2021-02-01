package com.ly.community53.controller;

import com.ly.community53.annotation.LoginRequired;
import com.ly.community53.entity.LoginTicket;
import com.ly.community53.entity.User;
import com.ly.community53.service.FollowService;
import com.ly.community53.service.LikeService;
import com.ly.community53.service.UserService;
import com.ly.community53.util.CommunityConstant;
import com.ly.community53.util.CommunityUtil;
import com.ly.community53.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community53.path.upload}")//服务器预设的头像存储路径
    private String uploadPath;

    @Value("${community53.path.domain}")//域名注入
    private String domain;

    @Value("${server.servlet.context-path}")//项目访问路径
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${quniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 上传文件名称：弄成随机的
        String fileName = CommunityUtil.generateUUID();
        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0)); //返回的是0就代表成功了
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        //上传凭证过期时间1h
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    // 更新头像路径，把网站的用户头像更新为七牛云的链接；异步请求来操作
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + fileName;//七牛云的链接
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }


    //    @Deprecated   该方法废弃了，用七牛云了
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";//一旦有错就返回设置页面
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));//图片文件名后缀
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);//这个dest用来上传到服务器的dest位置
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径) ——> userService.updateHeader(user.getId(), headerUrl);
        // http://localhost:8080/community53/user/header/xxx.png   需要提供给user的是web路径。
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);  //这个headUrl就是头像的headerUrl

        return "redirect:/index";  //  /index  这是url，而非跳到某个html页面
    }

    //    @Deprecated   该方法废弃了，用七牛云了
    //获取图片  //头像的headerUrl路径是http://localhost:8080/community53/user/header/{fileName}"，就来到这个方法，response就会返回图片
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放头像图片的路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (//图片是二进制数据，用到字节流
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);//buffer里的数据写到哪了？写给response
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }//流不关闭？？ fis、os写在了try的（ ）里；jdk7的语法会自动关闭
    }

    //修改用户密码
    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, String confirmPassword, Model model){

        if(oldPassword == null){
            model.addAttribute("errorOld", "原密码输入为空");
            return "/site/setting";//一旦有错就返回设置页面
        }
        if(newPassword == null){
            model.addAttribute("errorNew", "新密码输入为空");
            return "/site/setting";//一旦有错就返回设置页面
        }
        //不能newPassword != confirmPassword；这个比较的对象地址。用equals
        if(!Objects.equals(newPassword, confirmPassword)){
            model.addAttribute("errorConfirm", "新密码与确认密码不一致");
            return "/site/setting";//一旦有错就返回设置页面
        }

        if(newPassword.length() < 8){
            model.addAttribute("errorNew", "密码长度不能小于8位!");
            return "/site/setting";//一旦有错就返回设置页面
        }

        User user = hostHolder.getUser();
        String oldPassword_md5 = CommunityUtil.md5(oldPassword + user.getSalt());
        //不能 != ；这个比较的对象地址。用equals
        if(!Objects.equals(user.getPassword(), oldPassword_md5)){
            model.addAttribute("errorOld", "原密码输入错误");
            return "/site/setting";//一旦有错就返回设置页面
        }

        //更改密码；这个搞到service层较好
        String newPassword_md5 = CommunityUtil.md5(newPassword + user.getSalt());
        userService.updatePassword(user.getId(),newPassword_md5);

        //直接跳转到退出页面不就行了。
        return "redirect:/logout";
    }

    // 个人主页(主页谁都可以看，可以看他人的个人主页，故url要有userId)  不登录也可访问
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // model存入该用户
        model.addAttribute("user", user);
        // 存入点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量：用户关注的实体的数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量：实体的粉丝的数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注：当前用户是否已关注该实体
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

}
