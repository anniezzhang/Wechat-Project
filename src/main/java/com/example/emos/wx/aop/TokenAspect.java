package com.example.emos.wx.aop;

import com.example.emos.wx.Config.shiro.ThreadLocalToken;
import com.example.emos.wx.common.util.R;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TokenAspect {
    @Autowired
    private ThreadLocalToken threadLocalToken;
    //cut all method under controller
    @Pointcut("execution(public * com.example.emos.wx.controller.*.*(..)))")
    public void aspect(){

    }

    @Around("aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        R r=(R)point.proceed(); //result returned from web method
        String token = threadLocalToken.getToken();
        //if there is token in threadlocal, means it's updated token
        if(token !=null){
            r.put("token",token);
            threadLocalToken.clear();
        }
        return r;
    }
}
