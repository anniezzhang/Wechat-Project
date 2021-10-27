package com.example.emos.wx.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.example.emos.wx.Config.SystemConstants;
import com.example.emos.wx.Config.shiro.JwtUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.controller.form.SearchMonthCheckinForm;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.crypto.hash.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@RequestMapping("/checkin")
@RestController
@Api("Sign-in Web Interface")
@Slf4j
public class CheckinController {
    @Autowired
    private JwtUtil jwtUtil;
    @Value("${emos.image-folder}")
    private String imageFolder;
    @Autowired
    private CheckinService checkinService;
    @Autowired
    private UserService userService;
    @Autowired
    private SystemConstants constants;

    @GetMapping("/validCanCheckIn")
    @ApiOperation("To check if user can sign in today")
    public R validCanCheckIn(@RequestHeader("token") String token ){
        int userId= jwtUtil.getUserId(token);
        String result=checkinService.validCanCheckIn(userId, DateUtil.today());
        return R.ok(result);
    }
    @PostMapping("/checkin")
    @ApiOperation("Sign-in")
    public R checkin(@Valid CheckinForm form, @RequestParam("photo") MultipartFile file,@RequestHeader String token) {
        if(file == null){
            return R.error("No photo uploaded.");
        }
        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase();
        if(!fileName.endsWith(".jpg")){
            return R.error("Photo must be in JPG format.");
        }else{
            String path = imageFolder + "/" + fileName;
            try {
                file.transferTo(Paths.get(path));
                HashMap param = new HashMap();
                param.put("userId", userId);
                param.put("path",path);
                param.put("address", form.getAddress());
                checkinService.checkin(param);
                return R.ok("Sign-in successful.");
            } catch (IOException e) {
                log.error(e.getMessage(),e);
                throw new EmosException("Error in saving photo.");
            }
            finally {
                FileUtil.del(path);
            }
        }

    }
    @PostMapping("/createFaceModel")
    @ApiOperation("Create Face Model")
    public R createFaceModel(@RequestParam("photo") MultipartFile file,@RequestHeader("token") String token){
        if(file == null){
            return R.error("No photo uploaded.");
        }
        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase();
        if(!fileName.endsWith(".jpg")){
            return R.error("Photo must be in JPG format.");
        }else{
            String path = imageFolder + "/" + fileName;
            try {
                file.transferTo(Paths.get(path));
                checkinService.creatFaceModel(userId,path);
                return R.ok("Face Model created successfully.");
            } catch (IOException e) {
                log.error(e.getMessage(),e);
                throw new EmosException("Error in saving photo.");
            }
            finally {
                FileUtil.del(path);
            }
        }
    }
    @GetMapping("/searchTodayCheckin")
    @ApiOperation("Search user current sign-in data")
    public R searchTodayCheckin(@RequestHeader("token") String token){
        int userId= jwtUtil.getUserId(token);
        HashMap map = checkinService.searchTodayCheckin(userId);
        map.put("attendanceTime",constants.attendanceTime);
        map.put("closingTime",constants.closingTime);
        long days = checkinService.searchCheckinDays(userId);
        map.put("checkinDays",days);

        DateTime hireDate = DateUtil.parse(userService.searchUserHireDate(userId));
        DateTime startDate = DateUtil.beginOfWeek(DateUtil.date());
        if(startDate.isBefore(hireDate)){
            startDate=hireDate;
        }
        DateTime endDate= DateUtil.endOfWeek(DateUtil.date());
        HashMap param=new HashMap();
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        param.put("userId",userId);
        ArrayList<HashMap> list = checkinService.searchWeekCheckin(param);
        map.put("weekCheckin",list);
        return R.ok().put("result",map);


    }
    @PostMapping("/searchMonthCheckin")
    @ApiOperation("Search user sign-in records for certain month")
    public R searchMonthCheck(@Valid @RequestBody SearchMonthCheckinForm form, @RequestHeader("token") String token){
        int userId = jwtUtil.getUserId(token);
        DateTime hiredate = DateUtil.parse(userService.searchUserHireDate(userId));
        String month = form.getMonth()<10?"0"+form.getMonth():form.getMonth().toString();
        DateTime startDate = DateUtil.parse(form.getYear()+"-"+month+"-01");
        if(startDate.isBefore(DateUtil.beginOfMonth(hiredate))){
            throw new EmosException("Only allowed to check records after hire date");
        }
        if(startDate.isBefore(hiredate)){
            startDate=hiredate;
        }
        DateTime endDate = DateUtil.endOfMonth(startDate);
        HashMap param = new HashMap();
        param.put("userId",userId);
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        ArrayList<HashMap> list = checkinService.searchMonthCheckin(param);
        int sum_1=0,sum_2=0,sum_3=0;
        for (HashMap<String, String> one : list){
            String type = one.get("type");
            String status = one.get("status");
            if("workday".equals(type)){
                if("Attended".equals(status)){
                    sum_1++;
                }else if ("Late".equals(status)){
                    sum_2++;
                }else if ("Absent".equals(status)){
                    sum_3++;
                }
            }
        }
        return R.ok().put("list",list).put("sum_1",sum_1).put("sum_2",sum_2).put("sum_3",sum_3);
    }

}
