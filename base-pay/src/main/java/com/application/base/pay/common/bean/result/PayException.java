package com.application.base.pay.common.bean.result;

/**
 *@desc 支付异常
 @author 孤狼
 */
public class PayException implements PayError {
    
    private String errorCode;
    private String errorMsg;
    private String content;

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    public PayException(String errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public PayException(String errorCode, String errorMsg, String content) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.content = content;
    }

    @Override
    public String getString() {
        return "支付错误: errcode=" + errorCode + ", errmsg=" + errorMsg + (null == content ? "" : "\n content:" + content);
    }
}