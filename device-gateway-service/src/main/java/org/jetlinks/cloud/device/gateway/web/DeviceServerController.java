package org.jetlinks.cloud.device.gateway.web;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.controller.message.ResponseMessage;
import org.jetlinks.gateway.monitor.GatewayServerMonitor;
import org.jetlinks.protocol.message.codec.Transport;
import org.jetlinks.registry.api.DeviceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/servers")
@RestController
public class DeviceServerController {

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private GatewayServerMonitor serverMonitor;


    @GetMapping("/{transport}")
    public ResponseMessage<List<String>> getDeviceServer(@PathVariable Transport transport) {
        return ResponseMessage.ok(serverMonitor.getCurrentServerInfo()
                .getTransportHosts(transport));
    }

}
