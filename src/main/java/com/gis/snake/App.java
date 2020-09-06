package com.gis.snake;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.gis.snake.controller"})
@MapperScan("com.gis.snake.mapper")
public class App {

    public static void  main(String[] args){
        SpringApplication.run(App.class,args);
    }

}
