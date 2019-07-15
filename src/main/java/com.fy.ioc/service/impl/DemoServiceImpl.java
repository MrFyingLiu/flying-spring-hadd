package com.fy.ioc.service.impl;

import com.fy.framework.annotation.FYService;
import com.fy.ioc.service.IDemoService;

@FYService
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        return " My name is "+name;
    }
}
