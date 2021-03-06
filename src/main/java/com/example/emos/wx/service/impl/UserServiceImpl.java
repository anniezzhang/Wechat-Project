package com.example.emos.wx.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

@Service
@Slf4j
@Scope("prototype")
public class UserServiceImpl implements UserService {
    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    @Autowired
    private TbUserDao userDao;

    private String getOpenId(String code){
        String url="https://api.weixin.qq.com/sns/jscode2session";
        HashMap map=new HashMap();
        map.put("appid",appId);
        map.put("secret",appSecret);
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String response = HttpUtil.post(url,map);
        JSONObject json = JSONUtil.parseObj(response);
        String openId=json.getStr("openid");
        if(openId==null||openId.length()==0){
            throw new RuntimeException("Login credentials error.");
        }
        return openId;

    }

    @Override
    public int registerUser(String registerCode, String code, String nickname, String photo) {
       // System.out.println("Hi, this is a test for registerCode= "+registerCode);
       // System.out.println("Hi, this is a test for code= "+code);
       // System.out.println("Hi, this is a test for nickname= "+nickname);
        if(registerCode.equals("000000")){
          boolean bool = userDao.haveRooterUser();
          if (!bool){
              String openId=getOpenId(code);
              HashMap param=new HashMap();
              param.put("openId",openId);
              param.put("nickname",nickname);
              param.put("photo",photo);
              param.put("role","[0]");
              param.put("status",1);
              param.put("createTime", new Date());
              param.put("root",true);
              userDao.insert(param);
              int id =userDao.searchIdByOpenId(openId);
              return id;
          }else{
              throw new EmosException("Cannot link to Super Admin Account");
          }
        }else{

        }

        return 0;
    }

    @Override
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions=userDao.searchUserPermissions(userId);
        return permissions;
    }

    @Override
    public Integer login(String code) {
        String openId=getOpenId(code);
        Integer id =userDao.searchIdByOpenId(openId);
        if(id==null){
            throw new EmosException("Account doesn't exist");
        }
        //TODO pull msg from queue
        return id;
    }

    @Override
    public TbUser searchById(int userId) {
        TbUser user=userDao.searchById(userId);
        return user;
    }

    @Override
    public String searchUserHireDate(int userId) {
        String hiredate = userDao.searchUserHireDate(userId);
        return hiredate;
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        HashMap map = userDao.searchUserSummary(userId);
        return map;
    }
}
