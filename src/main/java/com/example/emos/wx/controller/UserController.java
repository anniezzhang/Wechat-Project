package com.example.emos.wx.controller;

import com.example.emos.wx.Config.shiro.JwtUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.LoginForm;
import com.example.emos.wx.controller.form.RegisterForm;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Api("User Module Web Interface")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;
    @PostMapping("/register")
    @ApiOperation("User Registration")
    public R register(@Valid @RequestBody RegisterForm form){
       int id = userService.registerUser(form.getRegisterCode(), form.getCode(), form.getNickname(), form.getPhoto());
        //System.out.println("Hi, this is a test for registerCode= "+form.getRegisterCode());
        //System.out.println("Hi, this is a test for code= "+form.getCode());
       // System.out.println("Hi, this is a test for nickname= "+form.getNickname());
       String token = jwtUtil.createToken(id);
       Set<String> permsSet=userService.searchUserPermissions(id);
       saveCacheToken(token,id);
       return R.ok("Registration Successful.").put("token",token).put("permissions",permsSet);
    }
    @PostMapping("/login")
    @ApiOperation("User Login")
    public R login(@Valid @RequestBody LoginForm form){
        int id = userService.login(form.getCode());
        String token = jwtUtil.createToken(id);
        saveCacheToken(token,id);
        Set<String> permsSet = userService.searchUserPermissions(id);
        System.out.println("permsSet is " + permsSet);
        return R.ok("Login successfully.").put("token",token).put("permissions",permsSet);
    }
    @GetMapping("/searchUserSummary")
    @ApiOperation("Search User Info")
    public R searchUserSummary(@RequestHeader("token") String token){
        int userId= jwtUtil.getUserId(token);
        HashMap map = userService.searchUserSummary(userId);
        return R.ok().put("result",map);
    }

    private void saveCacheToken(String token, int userId){
        redisTemplate.opsForValue().set(token,userId+"", cacheExpire, TimeUnit.DAYS);
    }
}
