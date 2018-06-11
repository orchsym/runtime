package com.baishancloud.orchsym.processors.dubbo.it;

import java.io.IOException;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author GU Guoqiang
 *
 */
public class InvokeDubboITProvider {
    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("provider.xml");
        context.start();
        System.out.println("Service started!");

        System.out.println("\nPress any key to exit the service");
        System.in.read();
        context.close();
    }
}
