//package com.ly.community53.aspect;
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.*;
//import org.springframework.stereotype.Component;
//
//@Component
//@Aspect
//public class AlphaAspect {
//
//    @Pointcut("execution(* com.ly.community53.service.*.*(..))")
//    public void pointcut() {
//
//    }
//
//    @Before("pointcut()")
//    public void before() {
//        System.out.println("before");
//    }
//
//    @After("pointcut()")
//    public void after() {
//        System.out.println("after");
//    }
//
//    @AfterReturning("pointcut()")  //AfterReturning、After的执行顺序？
//    public void afterRetuning() {
//        System.out.println("afterRetuning");
//    }
//
//    @AfterThrowing("pointcut()")
//    public void afterThrowing() {
//        System.out.println("afterThrowing");
//    }
//
//    @Around("pointcut()")
//    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
//        System.out.println("around before");
//        Object obj = joinPoint.proceed();//调用目标组件的方法，其可能有返回值
//        System.out.println("around after");
//        return obj;
//    }
//
//}
