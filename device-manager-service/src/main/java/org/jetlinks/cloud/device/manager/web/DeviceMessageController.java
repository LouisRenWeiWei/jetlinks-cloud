package org.jetlinks.cloud.device.manager.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import org.hswebframework.web.authorization.annotation.Authorize;
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

    @GetMapping("/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @Authorize(permission = "device-manager", action = "read-property", description = "获取设备属性")
    @ApiOperation("获取设备属性信息")
    public ResponseMessage<?> getProperties(@PathVariable String deviceId, @PathVariable String property) {
        return registry.getDevice(deviceId)//从注册中心获取设备操作接口
                .messageSender()
                .readProperty(property.split("[, ;]"))//构造读取属性请求
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .send() //发送消息到设备
                .toCompletableFuture()
                .thenApply(reply -> {
                    if (reply.isSuccess()) {
                        //成功
                        return ResponseMessage.ok(reply.getProperties());
                    } else {
                        return ResponseMessage.error(500, reply.getMessage()).code(reply.getCode());
                    }
                })
                .get(10, TimeUnit.SECONDS); //最大等待10秒
    }

}
