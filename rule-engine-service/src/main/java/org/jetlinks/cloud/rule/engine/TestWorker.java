package org.jetlinks.cloud.rule.engine;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.rule.engine.api.RuleData;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class TestWorker {

    public Object updateDeviceProperty(String deviceId, Map<String,Object> parameter){

        log.debug("修改设备[{}]属性:{}",deviceId,parameter);
        return true;
    }

    public void handlerReadFail(RuleData ruleData){

        log.debug("读取设备属性错误:{}",ruleData.toString());
    }
}
