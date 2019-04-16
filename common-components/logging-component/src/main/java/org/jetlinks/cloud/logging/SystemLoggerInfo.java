package org.jetlinks.cloud.logging;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class SystemLoggerInfo implements Serializable {
    private String serverId;

    private String host;

    private String module;

    private String sourceCode;

    private String message;

    private String level;

    private String className;

    private String methodName;

    private int lineNumber;

    private String stack;
}
