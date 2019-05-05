package org.jetlinks.cloud.device.manager;

import org.hswebframework.web.dao.Dao;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

@SpringBootApplication
@SpringBootTest
@WebAppConfiguration
@MapperScan(value = "org.jetlinks.cloud.device.manager.dao",markerInterface = Dao.class)
public class TestApplication {



}
