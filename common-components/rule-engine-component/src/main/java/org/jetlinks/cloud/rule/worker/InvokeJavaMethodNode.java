package org.jetlinks.cloud.rule.worker;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.ExpressionUtils;
import org.jetlinks.rule.engine.executor.supports.JavaMethodInvokeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@Slf4j
public class InvokeJavaMethodNode extends JavaMethodInvokeStrategy {

    @Autowired
    private ApplicationContext context;

    @Override
    public Object getInstance(Class type) {
        try {
            return context.getBean(type);
        } catch (Exception e) {
            log.debug("从spring获取类实例失败", e);
        }
        return super.getInstance(type);
    }

    @Override
    @SneakyThrows
    protected Object convertParameter(String parameter, Map<String, Object> mapData) {
        Object val= super.convertParameter(parameter, mapData);
        if(val==null){
            return ExpressionUtils.analytical(parameter,mapData,"spel");
        }
        return val;
    }
}
