package com.example.emos.wx.controller;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.TestSayHelloForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/test")
@Api("Test Web Interface")
public class TestController {
    @PostMapping("/sayHello")
    @ApiOperation("An easy test")
    public R sayHello(@Valid @RequestBody TestSayHelloForm form){
        return R.ok().put("message","Hello, "+form.getName());
    }
    @PostMapping("/addUser")
    @ApiOperation("Add user")
    @RequiresPermissions(value = {"A","B"},logical = Logical.OR)
    public R addUser(){
        return R.ok("User added successfully");
    }
}
