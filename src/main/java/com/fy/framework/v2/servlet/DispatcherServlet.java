package com.fy.framework.v2.servlet;

import com.fy.framework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {

    private Properties configs = new Properties();

    private List<String > classNames= new ArrayList<String>();
private Map<String,Object> ioc = new ConcurrentHashMap<String,Object>();
private List<HandlerMaing> hadderMapping = new ArrayList<HandlerMaing>();
    private List<Handler> hadderMapp = new ArrayList<Handler>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //派遣，分发任务
        try {
            //委派模式
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Excetion Detail:" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
       Handler handlerMaing=   getHandler(req);
        if (null ==handlerMaing ) {
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Method method =handlerMaing.getMethod();// this.hadderMapping.get(url);
        //取到传入 进来 的参数
        Map<String, String[]> params = req.getParameterMap();
        //取到 这个方法需要 哪几个参数 ，
        Class<?>[] parameterTypes = handlerMaing.getParamTypes();
        Object[] paraValues = new Object[parameterTypes.length];
      for(Map.Entry<String,String[]> parm : params.entrySet()) {
          String value = Arrays.toString(parm.getValue()).replaceAll("\\[|\\]", "")
                  .replaceAll("\\s", "");
          if (!handlerMaing.getParamIndexMapping().containsKey(parm.getKey())) continue;
          int index = handlerMaing.getParamIndexMapping().get(parm.getKey());
          paraValues[index] = convert(parameterTypes[index], value);

          if(handlerMaing.getParamIndexMapping().containsKey(HttpServletRequest.class.getName())) {
              int reqindex = handlerMaing.getParamIndexMapping().get(HttpServletRequest.class.getName());
              paraValues[reqindex] = req;
          }
          if(handlerMaing.getParamIndexMapping().containsKey(HttpServletResponse.class.getName())) {
              int rspindex = handlerMaing.getParamIndexMapping().get(HttpServletResponse.class.getName());
              paraValues[rspindex] = resp;
          }
      }
        Object res =    handlerMaing.getMethod().invoke(handlerMaing.getInstance(),paraValues);
     if(res!=null &&  !res.equals("")){
         resp.getWriter().write(res.toString());
     }

    }

    private Object convert(Class<?> type , String value) {
        if(Double.class ==type){
            return new Double(value);
        }else if(Short.class ==type){
            return new Short(value);
        }
        else   if(Integer.class==type){
           // if(value.length()<10) {
                return new Integer(value);
           // }
        }
        return value;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //加载 配置文件
        doLoadConfig( config.getInitParameter("contextConfigLocation"));
        //2、扫描相关的类
        doScanner(configs.getProperty("scanPackage"));

        doInstance();

        doAutoWirted();

        initHanddleMapping();

        System.out.println("fy Spring framework is init.");
    }

    //把 所有 控制 类 中的url 进行处理
    private void initHanddleMapping() {
        if (ioc.isEmpty()) return;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(FYController.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(FYRequestMapping.class)) {
                FYRequestMapping fyRequestMapping = clazz.getAnnotation(FYRequestMapping.class);
                baseUrl += fyRequestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(FYRequestMapping.class)) continue;

                FYRequestMapping fyRequestMapping = method.getAnnotation(FYRequestMapping.class);
              //  baseUrl = baseUrl + "/" + fyRequestMapping.value();
                String regex = ("/" + baseUrl + "/" + fyRequestMapping.value()).replaceAll("/+", "/");

                Pattern pattern = Pattern.compile(regex);
                hadderMapp.add( new Handler(entry.getValue(),method,pattern));
            }
        }

    }

    //自动 注入 实现实例化
    private void doAutoWirted() {
        if(ioc.isEmpty()) return;
//把注解 的类中的所有 的成员变量拿出来 ， 如果 有需要 自动 注入的 属性时   就给他自动注入
        for(Map.Entry<String,Object> entry:  ioc.entrySet()){
            //获取所有的 属性 ，包括私有的
           Field [] fields =  entry.getValue().getClass().getDeclaredFields();
           for(Field field : fields){
               if(!field.isAnnotationPresent(FYAutoWirted.class)){    continue;    }
               FYAutoWirted fyAutoWirted =    field.getAnnotation(FYAutoWirted.class);
               String beanName = fyAutoWirted.value();
                if(beanName.equals("")){
                    beanName = field.getType().getName();
                }
                //私有属性的权限 需要  强制 操作才能成功，
               field.setAccessible(true);
               try {
                   //进行注入操作
                   //把这个对象中的属性的值  进行实例化 ，也就是从ioc中取出来 赋值
                   field.set(entry.getValue(),ioc.get(beanName));
               } catch (IllegalAccessException e) {
                   e.printStackTrace();
                   continue;
               }
           }
        }
    }

    //初始化已经完成  把加了注解 的类先加载 进来
    private void doInstance() {
        if (classNames.isEmpty()) return ;
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(FYController.class)) {
                    Object instance = clazz.newInstance();
                    String BeanName = tofirstLowser(clazz.getSimpleName());
                    ioc.put(BeanName,instance);
                }else if(clazz.isAnnotationPresent(FYService.class)){


                    FYService fyService = clazz.getAnnotation(FYService.class);
                    String  BeanName = fyService.value();
                    if(BeanName.trim().equals("")){
                        BeanName = tofirstLowser(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(BeanName,instance);

                    //把这个类的所有的 接口都扫描 进来 ，用 他的名字做key
                    for(Class<?> i : clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The beanName is exists!!");
                        }
                        ioc.put(i.getName(),instance);
                    }

                }else{
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String tofirstLowser(String name ){
        char[] chars = name.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }

    private void doScanner(String  scanPackage) {
        URL url = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());
        for(File file : classPath.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else {
                if(!file.getName().endsWith(".class")) continue;
                String className = scanPackage+ "."+ file.getName().replaceAll(".class","");
                classNames.add(className);
            }
        }


    }

    private void doLoadConfig(String contextConfigLocation) {

        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            configs.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null!=fis){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private HandlerMaing getHandlerMap(HttpServletRequest req ){
        if(hadderMapping.isEmpty())return null;
        String url = req.getRequestURI();
     String contextpath = req.getContextPath();
        url = url.replace(contextpath,"").replaceAll("/+","/");
        for(HandlerMaing handler : hadderMapping){
           // Matcher matcher = handler.pattern.matcher(url);
            if(handler.getUrl().equals(url)){
                return handler;
            }
        }
        return null;
    }
    private Handler getHandler(HttpServletRequest req ){
        if(hadderMapp.isEmpty())return null;
        String url = req.getRequestURI();
        String contextpath = req.getContextPath();
        url = url.replace(contextpath,"").replaceAll("/+","/");
        for(Handler handler : hadderMapp){
            Matcher matcher = handler.pattern.matcher(url);
            if(!matcher.matches()){
                continue;
            }
            return handler;
        }
        return null;
    }
    public class HandlerMaing{
        private String url ;
        private Method method;
        private Object instance;
        private Map<String ,Integer > paramIndexMapping;
        private Class<?>[] paramTypes;

        public String getUrl() {
            return url;
        }

        public Method getMethod() {
            return method;
        }

        public Object getInstance() {
            return instance;
        }

        public Map<String, Integer> getParamIndexMapping() {
            return paramIndexMapping;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        public HandlerMaing(String url, Object controller, Method method) {
            this.url = url;
            this.method = method;
            this.instance = controller;
            paramTypes = method.getParameterTypes();
            paramIndexMapping = new HashMap<String, Integer>();
            putIndexMapping(method);
        }
        private void putIndexMapping(Method method) {
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof FYRequestParms) {
                        String paramName = ((FYRequestParms) a).value();
                        if (!paramName.equals("")) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            Class<?>[] parameterTypes = method.getParameterTypes();

            for (int i = 0; i < parameterTypes.length; i++) {
                Class param = parameterTypes[i];
                if (param == HttpServletRequest.class || param == HttpServletResponse.class) {
                    paramIndexMapping.put(param.getName(), i);
                }
            }
        }
    }

    public class Handler {
        protected Object instance;
        protected Method method;
        private Pattern pattern;
        protected Map<String, Integer> paramIndexMapping;
        private Class<?>[] paramTypes;

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        public Object getInstance() {
            return instance;
        }

        public Method getMethod() {
            return method;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public Map<String, Integer> getParamIndexMapping() {
            return paramIndexMapping;
        }

        public Handler(Object instance, Method method, Pattern pattern ) {
            this.instance = instance;
            this.method = method;
            this.pattern = pattern;
            paramTypes = method.getParameterTypes();
            paramIndexMapping = new HashMap<String, Integer>();
            putIndexMapping(method);
        }

        private void putIndexMapping(Method method) {
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof FYRequestParms) {
                        String paramName = ((FYRequestParms) a).value();
                        if (!paramName.equals("")) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            Class<?>[] parameterTypes = method.getParameterTypes();

            for (int i = 0; i < parameterTypes.length; i++) {
                Class param = parameterTypes[i];
                if (param == HttpServletRequest.class || param == HttpServletResponse.class) {
                    paramIndexMapping.put(param.getName(), i);
                }
            }
        }
    }

}
