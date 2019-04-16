package org.jetlinks.cloud.logging;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.logging.AccessLoggerInfo;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class SerializableAccessLoggerInfo implements Serializable {
    private String id;

    private String serverId;

    private String action;

    private String describe;

    private String url;

    private String className;

    private String methodName;

    private String httpMethod;

    private String requestJson;

    private String ip;

    private static final Class excludes[] = {
            ServletRequest.class,
            ServletResponse.class,
            InputStream.class,
            OutputStream.class,
            MultipartFile.class,
            MultipartFile[].class,
            Authentication.class
    };

    public static SerializableAccessLoggerInfo of(AccessLoggerInfo loggerInfo) {
        SerializableAccessLoggerInfo accessLoggerInfo = new SerializableAccessLoggerInfo();
        accessLoggerInfo.id = loggerInfo.getId();
        accessLoggerInfo.action = loggerInfo.getAction();
        accessLoggerInfo.describe = loggerInfo.getDescribe();
        accessLoggerInfo.className = loggerInfo.getTarget().getName();
        accessLoggerInfo.methodName = loggerInfo.getMethod().getName();
        accessLoggerInfo.httpMethod = loggerInfo.getHttpMethod();
        accessLoggerInfo.url = loggerInfo.getUrl();
        accessLoggerInfo.ip = loggerInfo.getIp();
        if (!CollectionUtils.isEmpty(loggerInfo.getParameters())) {
            accessLoggerInfo.requestJson = JSON.toJSONString(loggerInfo
                    .getParameters()
                    .entrySet()
                    .stream()
                    .filter(entry -> Arrays.stream(excludes).noneMatch(clazz -> clazz.isInstance(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return accessLoggerInfo;

    }

}
