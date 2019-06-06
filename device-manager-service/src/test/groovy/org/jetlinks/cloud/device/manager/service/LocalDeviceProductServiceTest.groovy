package org.jetlinks.cloud.device.manager.service

import org.jetlinks.cloud.device.manager.TestApplication
import org.jetlinks.cloud.device.manager.entity.DeviceProductEntity
import org.jetlinks.core.device.registry.DeviceRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration
@SpringBootTest(classes = [TestApplication.class], properties = ["classpath:application.yml"])
class LocalDeviceProductServiceTest extends Specification {

    @Autowired
    private LocalDeviceProductService localDeviceProductService;

    @Autowired
    private DeviceRegistry registry;

    def "增删改查测试"() {
        def entity =new DeviceProductEntity(
                id: "test", name: "测试",
                projectId: "test-project",projectName: "测试项目",
                messageProtocol: "jetlinks",metadata: "{}"
        )

        given:
        localDeviceProductService.insert(entity)
        def product =  registry.getProduct(entity.getId());
        expect:
        product!=null
        product.getInfo()!=null
        product.getInfo().getProtocol() =='jetlinks'

    }
}
