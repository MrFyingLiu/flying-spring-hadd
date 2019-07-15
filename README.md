# flying-spring-hadd


# Mini版本的SpringMVC

基本思路 ：
	1.配置阶段：
			 配置web.xm 和application.properties 
			 配置注解 @controller @service  @autowrited  @requestmapping  @requestparam
			
	2.初始化阶段：
		 重写sevlet中init（）方法 
		 	加载配置文件 ，
			ioc容器初始化Map， 
			 扫描配置中对应的文件中的所有类 
			 对加了注解的类创建实例并放入ioc容器中
			 进行di操作，扫描ioc容器中的实例，给没有赋值的属性进行自动赋值
			 初始化handdlemapp，把url和一个handdle进行一对一关联，支持url正则表达式
			  handdle包括当前的实例、实例中的方法，方法的参数，方法的url正则
	
	3.运行阶段：
		 web容器调用get/post方法，获取request/response对象 
		 从request中获取url对象 ，在haddlemap中找到对应的method
		 利用反射调用 方法并返回结果
		 将结果输出给浏览器
		 

