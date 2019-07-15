package com.fy.ioc.mvc.action;

import com.fy.framework.annotation.FYAutoWirted;
import com.fy.framework.annotation.FYController;
import com.fy.framework.annotation.FYRequestMapping;
import com.fy.framework.annotation.FYRequestParms;
import com.fy.ioc.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@FYController
@FYRequestMapping("/index")
public class DemoAction {

    @FYAutoWirted
    private IDemoService demoService;

    @FYRequestMapping("query/.*")
    public  void select (HttpServletRequest req, HttpServletResponse res ,@FYRequestParms("name")String  name){
        String str = demoService.get(name);
//String str  = "my name is "+name;
        try {
            res.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FYRequestMapping("add")
    public  void add (HttpServletRequest req, HttpServletResponse res ,@FYRequestParms("a")Integer  a,@FYRequestParms("b")Integer  b){

        try {
            res.getWriter().write(a+"+"+b+"="+(a+b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FYRequestMapping("sub")
    public  void sub (HttpServletRequest req, HttpServletResponse res ,@FYRequestParms("a")Double  a,@FYRequestParms("b")Double  b){

        try {
            res.getWriter().write(a+"-"+b+"="+(a-b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FYRequestMapping("remove")
    public  String remove (@FYRequestParms("id")Integer  id){
      return ""+id;
    }

}
