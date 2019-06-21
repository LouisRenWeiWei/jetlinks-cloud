package org.jetlinks.cloud.device.manager.enums;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.cloud.DeviceConfigKey;
import org.jetlinks.core.Configurable;

import java.util.Collections;

@Getter
@AllArgsConstructor
public enum DeviceFeature implements EnumDict<String> {
    DEFAULT("默认"){
        @Override
        public void doConfig(Configurable configurable) {
            //默认推送设备上下线消息到消息队列
            configurable.put(DeviceConfigKey.deviceConnectTopic.getValue(),
                    new JSONArray(Collections.singletonList("device.connect")).toJSONString());

            configurable.put(DeviceConfigKey.deviceDisconnectTopic.getValue(),
                    new JSONArray(Collections.singletonList("device.disconnect")).toJSONString());
        }
    }

    ;

    private String text;

    @Override
    public String getValue() {
        return name();
    }

    public abstract void doConfig(Configurable configurable);


}
