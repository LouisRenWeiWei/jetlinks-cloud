package org.jetlinks.cloud.device.gateway.web;

import org.apache.commons.lang.math.RandomUtils;
import org.hswebframework.web.controller.message.ResponseMessage;
import org.jetlinks.gateway.monitor.GatewayServerInfo;
import org.jetlinks.gateway.monitor.GatewayServerMonitor;
import org.jetlinks.protocol.message.codec.Transport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RequestMapping("/dns")
@RestController
public class GateWayServerDnsController {

    private Selector defaultSelector = Selector.random;

    @Autowired
    private GatewayServerMonitor serverMonitor;

    public enum Selector {
        //永远获取第一个
        first() {
            @Override
            List<String> select(List<GatewayServerInfo> serverInfo, Transport transport) {
                return serverInfo.get(0).getTransportHosts(transport);
            }
        },
        //随机选取
        random() {
            @Override
            List<String> select(List<GatewayServerInfo> serverInfo, Transport transport) {
                return serverInfo.get(RandomUtils.nextInt(serverInfo.size()))
                        .getTransportHosts(transport);
            }
        },
        //选择最空闲的服务器
        moustIdle() {
            @Override
            List<String> select(List<GatewayServerInfo> serverInfo, Transport transport) {
                GatewayServerInfo target = null;
                long tmp = Long.MAX_VALUE;
                for (GatewayServerInfo info : serverInfo) {
                    long total = info.getDeviceConnectionTotal(transport);
                    if (total < tmp) {
                        tmp = total;
                        target = info;
                    }
                }
                if (target == null) {
                    return serverInfo.get(0).getTransportHosts(transport);
                }
                return target.getTransportHosts(transport);
            }
        };

        abstract List<String> select(List<GatewayServerInfo> serverInfo, Transport transport);
    }

    @GetMapping("/{transport}/{selector}")
    public ResponseMessage<List<String>> getAliveServer(@PathVariable String transport,
                                                        @PathVariable Selector selector) {

        Transport _transport = Arrays.stream(Transport.values())
                .filter(e -> e.name().toUpperCase().equals(transport.toUpperCase()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("不支持的协议:" + transport));

        List<GatewayServerInfo> allServers = serverMonitor.getAllServerInfo();

        if (allServers.isEmpty()) {
            return ResponseMessage.ok();
        }
        if (allServers.size() == 1) {
            return ResponseMessage.ok(allServers.get(0).getTransportHosts(_transport));
        }
        return ResponseMessage.ok(selector.select(allServers, _transport));
    }

    @GetMapping("/{transport}")
    public ResponseMessage<List<String>> getAliveServer(@PathVariable String transport) {
        return getAliveServer(transport, defaultSelector);
    }


}
