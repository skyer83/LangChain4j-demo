package com.lulala.langchain4j.jsonschema.service;

import dev.langchain4j.model.output.structured.Description;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/9/3 11:24
 */
public interface PersonExtractor1 {

    @Description("一个人")
    class Person {

        @Description("姓名")
        String name;

        @Description("年龄")
        int age;

        @Description("身高")
        Double height;

        @Description("是否已婚")
        boolean married;

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", height=" + height +
                    ", married=" + married +
                    '}';
        }
    }

    Person extractPersonFrom(String text);
}
