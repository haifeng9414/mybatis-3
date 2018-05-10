package com.dhf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MyTest {
    public static void consumer(Interface iface) {
        iface.doSomething();
    }

    public static void main(String[] args) {
        RealObject real = new RealObject();
        consumer(real);
        // Insert a proxy and call again:
        Interface proxy = (Interface) Proxy.newProxyInstance(//这里返回的不是我们通常认为的那个Interface接口实现对象，这是java为我们创建的一个继承了Proxy类，实现了newProxyInstance中接口列表的一个类，该类中实现每个接口的方法，实现中调用父类的 InvocationHandler 成员也就是我们的实现了 InvocationHandler 接口的对象的invoke方法
                Interface.class.getClassLoader(),
                new Class[]{Interface.class},
                new DynamicProxyHandler(real));
        consumer(proxy);
    }

    interface Interface {
        void doSomething();
    }

    static class RealObject implements Interface {
        public void doSomething() {
            System.out.println("RealObject do something");
        }
    }

    static class DynamicProxyHandler implements InvocationHandler {
        private Object proxied;//这个对象必须要有，供method.invoke用

        public DynamicProxyHandler(Object proxied) {//动态代理代理的是哪个关键在于这里传进来的是什么对象和newProxyInstance中接口为哪个对象实现的接口
            this.proxied = proxied;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("do something");
            return method.invoke(proxied, args); //注意！！！这里调用的是proxied而不是proxy，如果用proxy会出现无限调用doSomething方法，看底下的说明得知，因为proxy这个object其实是Proxy的newProxyInstance所返回的对象，在invoke中用proxy则相当于invoke中自己调用自己
        }
    }
}
