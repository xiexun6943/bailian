package com.ydd.zhichat.volley;


public class ObjectResult<T> extends Result {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
