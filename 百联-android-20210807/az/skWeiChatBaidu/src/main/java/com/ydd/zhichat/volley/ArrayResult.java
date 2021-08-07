package com.ydd.zhichat.volley;

import java.util.List;

public class ArrayResult<T> extends Result {
    private List<T> data;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
