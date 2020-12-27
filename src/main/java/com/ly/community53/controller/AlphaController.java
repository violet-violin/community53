package com.ly.community53.controller;

import com.ly.community53.service.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    // POST请求；用表单提交——static.html下student.html
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
}
