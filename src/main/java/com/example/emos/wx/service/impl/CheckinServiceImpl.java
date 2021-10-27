package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.Config.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Scope("prototype")
@Slf4j
public class CheckinServiceImpl implements CheckinService {
    @Autowired
    private SystemConstants constants;
    @Autowired
    private TbHolidaysDao tbHolidaysDao;
    @Autowired
    private TbWorkdayDao tbWorkdayDao;
    @Autowired
    private TbCheckinDao tbCheckinDao;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private TbFaceModelDao faceModelDao;
    @Value("${emos.face.createFaceModelUrl}")
    private String creatFaceModelUrl;
    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;
    @Value("${emos.email.hr}")
    private String hrEmail;
    @Autowired
    private EmailTask emailTask;

    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 =tbHolidaysDao.searchTodayIsHolidays()!=null?true:false;
        boolean bool_2 =tbWorkdayDao.searchTodayIsWorkday()!=null?true:false;
        String type = "Workday";
        if (DateUtil.date().isWeekend()){
            type="Holiday";
        }
        if (bool_1){
            type="Holiday";
        }else if (bool_2){
            type="Workday";
        }
        if(type.equals("Holiday")){
            return "No attendance required for Holiday.";
        }else{
            DateTime now = DateUtil.date();
            String start = DateUtil.today()+" "+ constants.attendanceStartTime;
            String end = DateUtil.today()+" "+constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if(now.isBefore(attendanceStart)){
                return "Not time for sign in for attendance yet.";
            }else if(now.isAfter(attendanceEnd)){
                return "Already past the time for sign in for attendance.";
            }else{
                HashMap map = new HashMap();
                map.put("userId",userId);
                map.put("date",date);
                map.put("start",start);
                map.put("end",end);
               boolean bool =  tbCheckinDao.haveCheckin(map)!=null?true:false;
               return bool?"Please don't sign-in more than once per day":"Valid sign-in time";
            }

        }

    }

    @Override
    public void checkin(HashMap param) {
        Date d1=DateUtil.date();
        Date d2=DateUtil.parse(DateUtil.today()+" "+constants.attendanceStartTime);
        Date d3=DateUtil.parse(DateUtil.today()+" "+constants.attendanceEndTime);
        int status = 1;
        if (d1.compareTo(d2)<=0){
            status=1;
        }else if (d1.compareTo(d2)>0 && d1.compareTo(d3)<0){
            status=2; //late sign-in
        }
        int userId = (Integer) param.get("userId");
        String faceModel = faceModelDao.searchFaceModel(userId);
        if (faceModel==null){
            throw new EmosException("Face model doesn't exist");
        }else{
            String path = (String)param.get("path");
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path),"targetModel",faceModel);
            //code?Purchase?
            HttpResponse response = request.execute();
            if(response.getStatus()!=200){
                log.error("Face recognization error.");
                throw new EmosException("Face recognization error.");
            }
            String body= response.body();
            if ("无法识别出人脸".equals(body)){
                throw new EmosException("Invalid sign-in. Not able to recognize the face model.");
            }else if ("照片中存在多张人脸".equals(body)){
                throw new EmosException("Invalid sign-in. Multiple faces detected.");
            }else if ("False".equals(body)){
                throw new EmosException("Invalid sign-in. Not employee himself/herself");
            }else if("True".equals(body)){
                //Save record. Check risk of Covid
                String address = (String )param.get("address");
                String result="";
                try {
                    if(address.contains("新泽西")){
                        String url = "https://www.worldometers.info/coronavirus/usa/new-jersey/";
                        Document document = Jsoup.connect(url).get();
                        Elements elements = document.getElementsByClass("total_row_usa");
                        if(elements.size()>0){
                            Element element = elements.get(1);
                            result = element.select("td:third-child").text();
                            System.out.println("Today's new cases is "+result);
                            //Send emails
                            HashMap<String,String> map = tbUserDao.searchNameAndDept(userId);
                            String name = map.get("name");
                            String deptName = map.get("dept_name");
                            deptName = deptName != null ? deptName : "";
                            SimpleMailMessage message = new SimpleMailMessage();
                            message.setTo(hrEmail);
                            message.setSubject("Employee" + name + "today's update for COVID");
                            message.setText("Department: " + deptName + "Employee: " + name + "\n"
                                    + DateUtil.format(new Date(), "MM/dd/yyyy") + "Located in " + address +"\n"
                                    + "New Covid cases is " + result);
                            emailTask.sendAsync(message);
                        }
                    }
                    if(address.contains("纽约")){

                    }
                } catch (Exception e) {
                        log.error("Running error",e);
                        throw new EmosException("Failed to get Covid new cases today");
                }
                //save sign-in record
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setStatus((byte)status);
                entity.setRisk(Integer.parseInt(result));
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                tbCheckinDao.insert(entity);

            }


        }
    }

    @Override
    public void creatFaceModel(int userId, String path) {
        HttpRequest request = HttpUtil.createPost(creatFaceModelUrl);//code?Purchase?
        request.form("photo",FileUtil.file(path));
        HttpResponse response = request.execute();
        String body = response.body();
        if("无法识别出人脸".equals(body)||"照片中存在多张人脸".equals(body)){
            throw new EmosException("Not able to recognize the face model.");
        }else{
            TbFaceModel entity = new TbFaceModel();
            entity.setUserId(userId);
            entity.setFaceModel(body);
            faceModelDao.insert(entity);
        }
    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map=tbCheckinDao.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days=tbCheckinDao.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        ArrayList<HashMap> checkinList=tbCheckinDao.searchWeekCheckin(param);
        ArrayList holidaysList=tbHolidaysDao.searchHolidaysInRange(param);
        ArrayList workdayList=tbWorkdayDao.searchWorkdayInRange(param);
        DateTime startDate=DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate=DateUtil.parseDate(param.get("endDate").toString());
        DateRange range=DateUtil.range(startDate,endDate, DateField.DAY_OF_MONTH);
        ArrayList<HashMap> list=new ArrayList<>();
        range.forEach(one->{
            String date=one.toString("MM-dd-yyyy");
            String type="workday";
            if(one.isWeekend()){
                type="holiday";
            }
            if(holidaysList!=null&&holidaysList.contains(date)){
                type="holiday";
            }
            else if(workdayList!=null&&workdayList.contains(date)){
                type="workday";
            }
            String status="";
            if(type.equals("workday")&&DateUtil.compare(one,DateUtil.date())<=0){
                status="Absent";
                boolean flag=false;//see if already signed in
                for (HashMap<String,String> map:checkinList){
                    if(map.containsValue(date)){
                        status=map.get("status");
                        flag=true;
                        break;
                    }
                }
                DateTime endTime=DateUtil.parse(DateUtil.today()+" "+constants.attendanceEndTime);
                String today=DateUtil.today();
                if(date.equals(today)&&DateUtil.date().isBefore(endTime)&&flag==false){
                    status="";
                }
            }
            HashMap map=new HashMap();
            map.put("date",date);
            map.put("status",status);
            map.put("type",type);
            map.put("day",one.dayOfWeekEnum());
            list.add(map);
        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }
}
