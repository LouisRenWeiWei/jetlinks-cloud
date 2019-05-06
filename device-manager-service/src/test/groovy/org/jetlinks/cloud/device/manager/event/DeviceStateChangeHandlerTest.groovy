package org.jetlinks.cloud.device.manager.event

import com.alibaba.fastjson.JSON
import org.jetlinks.cloud.device.manager.TestApplication
import org.jetlinks.cloud.device.manager.entity.DeviceInstanceEntity
import org.jetlinks.cloud.device.manager.enums.DeviceFeature
import org.jetlinks.cloud.device.manager.event.dispacher.DeviceOnlineOfflineConsumer
import org.jetlinks.cloud.device.manager.service.LocalDeviceInstanceService
import org.jetlinks.protocol.device.DeviceState
import org.jetlinks.registry.api.DeviceRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration
@SpringBootTest(classes = [TestApplication.class], properties = ["classpath:application.yml"])
class DeviceStateChangeHandlerTest extends Specification {

    @Autowired
    private MessageCollector messageCollector;

    @Autowired
    private BinderAwareChannelResolver resolver;

    @Autowired
    private LocalDeviceInstanceService instanceService;

    @Autowired
    private DeviceOnlineOfflineConsumer consumer;

    @Autowired
    private DeviceRegistry registry;

    def "测试消费设备上下线消息"() {
        given: "注册新设备"
        instanceService.insert(new DeviceInstanceEntity(
                id: "test", name: "测试设备",
                creatorId: "test", createTime: System.currentTimeMillis(), creatorName: "test",
                productId: "test", prodcutName: "test",
                state: DeviceState.noActive
        ))
        def operation = registry.getDevice("test");

        DeviceFeature.DEFAULT.doConfig(operation);

        when:
        operation != null
        operation.getDeviceInfo() != null
        then: "修改设备状态并发送消息到MQ"

        operation.online("test", "test")

        consumer.deviceOnline()
                .send(MessageBuilder.withPayload(JSON.toJSONString(["deviceId": "test"])).build());

        def instance = instanceService.selectByPk("test")
        expect: "设备状态成功修改"
        instance != null
        instance.getState() == DeviceState.online
    }

}
