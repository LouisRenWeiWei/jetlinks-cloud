package org.jetlinks.cloud.device.manager.web;

import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import org.hswebframework.web.controller.message.ResponseMessage;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.cloud.device.manager.service.LocalDeviceInstanceService;
import org.jetlinks.registry.api.DeviceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/device")
@Api(tags = "设备控制接口", description = "设备控制接口")
public class DeviceMessageController {

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private LocalDeviceInstanceService localDeviceInstanceService;

    @PutMapping("/state/sync")
    public ResponseMessage<Void> syncState(@RequestBody List<String> deviceIdList) {

        localDeviceInstanceService.syncState(deviceIdList, true);

        return ResponseMessage.ok();
    }

    //device/test1/property/test;test2;test3
    @GetMapping("/{deviceId}/property/{property:.+}")
    @SneakyThrows
    public ResponseMessage<?> getProperties(@PathVariable String deviceId, @PathVariable String property) {
        return registry.getDevice(deviceId)
                .messageSender()
                .readProperty(property.split("[, ;]"))
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .send()
                .toCompletableFuture()
                .thenApply(reply->{
                    if(reply.isSuccess()){
                        return ResponseMessage.ok(reply.getProperties());
                    }else{
                        return ResponseMessage.error(500,reply.getMessage()).code(reply.getCode());
                    }
                })
                .get(10, TimeUnit.SECONDS);
    }

}
