package com.miaoshaproject.controller;

import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.mysql.jdbc.util.Base64Decoder;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.prefs.BackingStoreException;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials="true",allowedHeaders="*")
public class UserController extends BaseController{


    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    //用户登录接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes ={CONTENT_TYPE_FORMED} )
    @ResponseBody
    public CommonReturnType register(@RequestParam(name="telephone")String telephone,
                                     @RequestParam(name="password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验
        if(StringUtils.isEmpty(telephone)||StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登录服务
        UserModel userModel=userService.validateLogin(telephone,this.EncodeByMd5(password));

        //将登录凭证加入到用户登陆成功的session内
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
        return CommonReturnType.creat(null);

        
    }

    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes ={CONTENT_TYPE_FORMED} )
    @ResponseBody
    public CommonReturnType register(@RequestParam(name="telephone")String telephone,
                                     @RequestParam(name="otpCode")String otpCode,
                                     @RequestParam(name="name")String name,
                                     @RequestParam(name="gender")Integer gender,
                                     @RequestParam(name="age")Integer age,
                                     @RequestParam(name="password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号和对应的otpcode相符
        String inSessionOtpCode= (String) httpServletRequest.getSession().getAttribute(telephone);
        if(!com.alibaba.druid.util.StringUtils.equals(otpCode,inSessionOtpCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机验证码不正确");
        }

        //用户注册流程
        UserModel userModel=new UserModel();
        userModel.setAge(age);
        userModel.setGender(gender);
        userModel.setTelephone(telephone);
        userModel.setRegisterMode("byphone");
        userModel.setName(name);
        userModel.setEncriptPassword(this.EncodeByMd5(password));
        userService.register(userModel);

        return CommonReturnType.creat(null);
    }

    public String EncodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5=MessageDigest.getInstance("MD5");
        Base64.Encoder base64en=Base64.getEncoder();

        //加密字符串
        String newstr=base64en.encodeToString(md5.digest(str.getBytes("utf-8")));
        return newstr;
    }



    //用户获取otp短信接口
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes ={CONTENT_TYPE_FORMED} )
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name="telephone")String telephone){
        //需要按照一定规则生成OTP验证码
        Random random=new Random();
        int randomInt=random.nextInt(99999);
        randomInt+=10000;
        String otpCode=String.valueOf(randomInt);


        //将OTP验证码同对应用户手机号关联,使用HTTP session的方式绑定手机号和OTPCode
        httpServletRequest.getSession().setAttribute(telephone,otpCode);

        //将OTP验证码通过短信通道发送给用户，先省略
        //下面这句话只是为了调试方便。在实际情况，绝对不能这么写，因为otpCode都是敏感信息，不能暴露给企业端
        System.out.println("telephone="+telephone+"&otpCode="+otpCode);

        return CommonReturnType.creat(null);
    }





    @RequestMapping("/get")
    @ResponseBody
public CommonReturnType getUser(@RequestParam(name="id")Integer id) throws BusinessException {
    //调用service服务获取对应id的用户的对象并返回给前端
    UserModel userModel=userService.getUserById(id);
    if(userModel==null){
        throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
    }

    UserVO userVO=convertFromModel(userModel);
    return CommonReturnType.creat(userVO);
}
private UserVO convertFromModel(UserModel userModel){
        if(userModel==null)
            return null;
        UserVO userVO=new UserVO();
    BeanUtils.copyProperties(userModel,userVO);
    return userVO;
}

}
