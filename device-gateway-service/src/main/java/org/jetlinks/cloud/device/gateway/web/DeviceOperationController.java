package org.jetlinks.cloud.device.gateway.web;

import lombok.SneakyThrows;
import org.hswebframework.web.controller.message.ResponseMessage;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.device.registry.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@RestController
@RequestMapping("/device")
public class DeviceOperationController {

    @Autowired
    private DeviceRegistry registry;

    @GetMapping("/{deviceId}/state")
    public ResponseMessage<Byte> checkDeviceState(@PathVariable String deviceId) {
        DeviceOperation operation = registry.getDevice(deviceId);
        operation.checkState();

        return ResponseMessage.ok(operation.getState());
    }

    @GetMapping("/{deviceId}/metadata")
    public ResponseMessage<String> getDeviceMetadata(@PathVariable String deviceId) {
        return ResponseMessage.ok(registry.getDevice(deviceId).getMetadata().toJson().toJSONString());
    }

    @GetMapping("/{deviceId}/property/{name}")
    @SneakyThrows
    public ResponseMessage<DeviceMessageReply> sendReadProperty(@PathVariable String deviceId,
                                                                @PathVariable String name) {
        String messageId = IDGenerator.MD5.generate();

        ReadPropertyMessageReply reply = registry.getDevice(deviceId)
                .messageSender()
                .readProperty(name)
                .messageId(messageId)
                .trySend(10, TimeUnit.SECONDS)
                .recover(TimeoutException.class, (err) ->
                        ReadPropertyMessageReply
                                .create()
                                .error(ErrorCode.TIME_OUT)
                                .messageId(messageId))
                .get();
        return ResponseMessage.ok(reply);
    }

    @PostMapping("/{deviceId}/invoke/{id}")
    @SneakyThrows
    public ResponseMessage<FunctionInvokeMessageReply> invokeFunction(@PathVariable String deviceId,
                                                                      @PathVariable String id,
                                                                      @RequestParam(required = false) Boolean async,
                                                                      @RequestBody List<FunctionParameter> input) {
        String messageId = IDGenerator.MD5.generate();

        FunctionInvokeMessageReply reply = registry
                .getDevice(deviceId)
                .messageSender()
                .invokeFunction(id)
                .messageId(messageId)
                .async(async)
                .setParameter(input)
                .messageId(IDGenerator.MD5.generate())
                .trySend(10, TimeUnit.SECONDS)
                .recover(TimeoutException.class, (err) ->
                        FunctionInvokeMessageReply
                                .create()
                                .error(ErrorCode.TIME_OUT)
                                .messageId(messageId))
                .get();
        return ResponseMessage.ok(reply);
    }

    @GetMapping("/{deviceId}/function/{messageId}")
    @SneakyThrows
    public ResponseMessage<FunctionInvokeMessageReply> invokeFunction(@PathVariable String deviceId,
                                                                      @PathVariable String messageId) {

        FunctionInvokeMessageReply reply = registry.getDevice(deviceId)
                .messageSender()
                .invokeFunction("_")//写啥都可以
                .messageId(messageId)
                .tryRetrieveReply(1, TimeUnit.SECONDS)
                .recover(TimeoutException.class, (err) ->
                        FunctionInvokeMessageReply
                                .create()
                                .error(ErrorCode.TIME_OUT)
                                .messageId(messageId))
                .get();

        return ResponseMessage.ok(reply);

    }
}
