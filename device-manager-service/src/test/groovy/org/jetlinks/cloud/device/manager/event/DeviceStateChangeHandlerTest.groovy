package org.jetlinks.cloud.device.manager.event

import org.jetlinks.cloud.device.manager.TestApplication
import org.jetlinks.cloud.device.manager.entity.DeviceInstanceEntity
import org.jetlinks.cloud.device.manager.enums.DeviceFeature
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
    private DeviceRegistry registry;

    def "测试消费设备上下线消息"() {
        given:
        instanceService.insert(new DeviceInstanceEntity(
                id: "test", name: "测试设备",
                creatorId: "test",createTime: System.currentTimeMillis(), creatorName: "test",
                productId: "test",prodcutName: "test",
                state: DeviceState.noActive
        ))
        def operation = registry.getDevice("test");

        DeviceFeature.DEFAULT.doConfig(operation);

        when:
        operation != null
        operation.getDeviceInfo() != null
        then:
        operation.online("test","test")
        messageCollector.forChannel(resolver.resolveDestination("device.connect"))
                .put(MessageBuilder.withPayload(["deviceId": "test"]).build())

        def instance = instanceService.selectByPk("test")
        expect:
        instance!=null
        instance.getState()== DeviceState.online
    }

}
