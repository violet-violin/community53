package com.ly.community53.controller;

import com.ly.community53.service.AlphaService;
import com.ly.community53.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author malaka
 * @create 2020-12-23 23:18
 */
@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @Autowired
    private AlphaService alphaService;

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello() {
        return "Hello Spring Boot.";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getData() {
        return alphaService.find();
    }

    //req、resp域
    @RequestMapping("/http")
    public void http(HttpServletRequest request, HttpServletResponse response) {
        // 获取请求数据
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());//得到请求行
        Enumeration<String> enumeration = request.getHeaderNames();//得到请求行的请求头（一组kv值）
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();
            String value = request.getHeader(name);
            System.out.println(name + ": " + value);
        }
        System.out.println(request.getParameter("code"));//得到req域的code的值

        // 返回响应数据
        response.setContentType("text/html;charset=utf-8");
        try (
                PrintWriter writer = response.getWriter();//java7语法——try后的（圆括号），自动关闭close方法
        ) {
            writer.write("<h1>牛客网</h1>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // GET请求

    //查询所有学生(分页为1、每页显示20条数据)： 访问路径 ————> /students?current=1&limit=20
    @RequestMapping(path = "/students", method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        System.out.println(current);
        System.out.println(limit);
        return "some students";
    }

    //按照id查询学生：访问路径 ————> /student/123          ；用的@PathVariable("id")  注解
    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id) {
        System.out.println(id);
        return "a student";
    }

    // POST请求；先url栏到达http://localhost:8080/html/student.html；再用表单提交——static下student.html
    //一般get缺陷：地址栏上明面传；地址栏长度有限
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, int age) {//controller方法如何获取表单中传入的数据？——声明的形参与表单的name一致即可
        System.out.println(name);
        System.out.println(age);
        return "success";
    }
    // 响应HTML数据：模板在 ——> template.demo下的view.html
    //会把ModelAndView传给模板引擎，进行渲染
    /**
     * <!--view.html如下-->
     * <!DOCTYPE html>
     * <!--xmlns:th="http://www.thymeleaf.org   即声明这是thymeleaf的模板，模板来源于thymeleaf官网-->
     * <html lang="en" xmlns:th="http://www.thymeleaf.org">
     * <head>
     *     <meta charset="UTF-8">
     *     <title>Teacher</title>
     * </head>
     * <body>
     * <!--th:text="${name}"   这是thymeleaf语法-->
     * <p th:text="${name}"></p>
     * <p th:text="${age}"></p>
     * </body>
     * </html>
     * @return
     */
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView getTeacher() {
        ModelAndView mav = new ModelAndView();
        mav.addObject("name", "张三");
        mav.addObject("age", 30);
        mav.setViewName("/demo/view");//设置模板的名字；thymeleaf默认后缀.html，前缀template
        return mav;
    }

    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model model) {
        model.addAttribute("name", "北京大学");
        model.addAttribute("age", 80);
        return "/demo/view";//返回的是视图的路径；这样形参就要有一个Model；第二种方式简单点。
    }

    // 响应JSON数据(异步请求)；异步请求——当前页面不刷新，但是悄悄访问了服务器数据并返回写信息，如检查昵称是否已用过
    // Java对象 -> JSON字符串 -> JS对象

    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    @ResponseBody     //返回json，要加该注解；  会把map转成json
    public Map<String, Object> getEmp() {
        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "张三");
        emp.put("age", 23);
        emp.put("salary", 8000.00);
        return emp;
    }
    //查询所有员工
    @RequestMapping(path = "/emps", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getEmps() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "张三");
        emp.put("age", 23);
        emp.put("salary", 8000.00);
        list.add(emp);

        emp = new HashMap<>();
        emp.put("name", "李四");
        emp.put("age", 24);
        emp.put("salary", 9000.00);
        list.add(emp);

        emp = new HashMap<>();
        emp.put("name", "王五");
        emp.put("age", 25);
        emp.put("salary", 10000.00);
        list.add(emp);

        return list;
    }

    // cookie示例

    @RequestMapping(path = "/cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response) {
        // 创建cookie
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        // 设置cookie生效的范围，声明cookie在哪些路径下有效
        cookie.setPath("/community53/alpha");
        //默认：cookie是存在浏览器的内存里，一旦关闭浏览器就没了。设置生存时间后会存在硬盘
        // 设置cookie的生存时间
        cookie.setMaxAge(60 * 10);
        // 发送cookie
        response.addCookie(cookie);

        return "set cookie";
    }

    @RequestMapping(path = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code) {//@CookieValue("code") ，从cookie中取key为code的值赋给形参
        System.out.println(code);
        return "get cookie";
    }

    //cookie局限：存在客户端，不安全；每次请求都带上，给服务器带来流量压力。
    //session局限：服务端内存压力大；   隐私数据就存session
    //用session,它不是http协议的标准，是JavaEE的标准
    // session示例

    @RequestMapping(path = "/session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session) {//springmvc自动创建session对象，就像model这些；直接声明一个形参用就行
        session.setAttribute("id", 1);//session存什么数据都行；cookie只能存少量数据、字符串
        session.setAttribute("name", "Test");
        return "set session";
    }

    @RequestMapping(path = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session) {//session对象也是springmvc自动注入
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session";
    }

    // ajax示例
    @RequestMapping(path = "/ajax", method = RequestMethod.POST)
    @ResponseBody
    public String testAjax(String name, int age) {
        System.out.println(name);
        System.out.println(age);
        return CommunityUtil.getJSONString(0, "操作成功!");
    }

}
