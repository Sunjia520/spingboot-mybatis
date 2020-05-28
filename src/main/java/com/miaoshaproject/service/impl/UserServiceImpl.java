package com.miaoshaproject.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dao.UserPasswordDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.dataobject.UserPasswordDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Override
    public UserModel getUserById(Integer id) {
        //调用userdomapper获取对应的用户dataobject
        UserDO userDO=userDOMapper.selectByPrimaryKey(id);
        if(userDO==null)
            return null;
        UserPasswordDO userPasswordDO=userPasswordDOMapper.selectByUserId(userDO.getId());

        return convertFromDataObject(userDO,userPasswordDO);
    }

    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        if(userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //对userModel的参数进行校验，不必再一个一个判空
        ValidationResult result=validator.validate(userModel);
        if(result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
        }


        //实现model->object
        UserDO userDO=convertFromModel(userModel);
        try {
            userDOMapper.insertSelective(userDO);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号码已重复注册");
        }

        userModel.setId(userDO.getId());
        UserPasswordDO userPasswordDO=convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);

        return;
    }

    @Override
    public UserModel validateLogin(String telephone, String encriptPassword) throws BusinessException {
        //通过用户手机获取用户信息
        UserDO userDO=userDOMapper.selectByTelephone(telephone);
        if(userDO==null){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPasswordDO userPasswordDO=userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel=convertFromDataObject(userDO,userPasswordDO);
        if(!StringUtils.equals(encriptPassword,userPasswordDO.getEncriptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;

    }

    private UserDO convertFromModel(UserModel userModel){
        if(userModel==null)
            return null;
        UserDO userDO=new UserDO();
        BeanUtils.copyProperties(userModel,userDO);
        return userDO;
    }
    private UserPasswordDO convertPasswordFromModel(UserModel userModel){
        if(userModel==null)
            return  null;
        UserPasswordDO userPasswordDO=new UserPasswordDO();
        userPasswordDO.setEncriptPassword(userModel.getEncriptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }

    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO){
        if(userDO==null)
            return null;
        UserModel userModel=new UserModel();
        BeanUtils.copyProperties(userDO,userModel);
        if(userPasswordDO!=null){
            userModel.setEncriptPassword(userPasswordDO.getEncriptPassword());
        }
        return userModel;
    }
}
