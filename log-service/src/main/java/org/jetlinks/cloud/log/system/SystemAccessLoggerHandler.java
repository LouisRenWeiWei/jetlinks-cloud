package org.jetlinks.cloud.log.system;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.cloud.logging.SerializableAccessLoggerInfo;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Component
@EnableBinding(SystemAccessLoggerConsumer.class)
@Slf4j
public class SystemAccessLoggerHandler {

    @StreamListener(SystemAccessLoggerConsumer.accessLogger)
    public void handleAccessLog(SerializableAccessLoggerInfo loggerInfo) {
        log.info(JSON.toJSONString(loggerInfo));
    }

}
