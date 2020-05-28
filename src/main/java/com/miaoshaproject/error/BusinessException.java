package com.miaoshaproject.error;

//包装器业务异常实现
public class BusinessException extends Exception implements CommonError {
    private CommonError commonError;

    public BusinessException(CommonError commonError){
        super();
        this.commonError=commonError;
    }

    public BusinessException(CommonError commonError,String errMsg){
        super();
        commonError.setErrMsg(errMsg);
        this.commonError=commonError;
    }

    @Override
    public int getErrCode() {
        return this.commonError.getErrCode();
    }

    @Override
    public String getErrMsg() {
        return this.commonError.getErrMsg();
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.commonError.setErrMsg(errMsg);
        return this;
    }
}
