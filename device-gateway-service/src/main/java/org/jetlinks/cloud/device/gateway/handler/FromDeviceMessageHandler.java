package org.jetlinks.cloud.device.gateway.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.cloud.device.gateway.events.ChildDeviceOfflineEvent;
import org.jetlinks.cloud.device.gateway.events.ChildDeviceOnlineEvent;
import org.jetlinks.cloud.device.gateway.events.DeviceOfflineEvent;
import org.jetlinks.cloud.device.gateway.events.DeviceOnlineEvent;
import org.jetlinks.cloud.device.gateway.vertx.DeviceMessageEvent;
import org.jetlinks.gateway.session.DeviceSession;
import org.jetlinks.gateway.session.DeviceSessionManager;
import org.jetlinks.protocol.device.DeviceOperation;
import org.jetlinks.protocol.message.event.ChildDeviceOfflineMessage;
import org.jetlinks.protocol.message.event.ChildDeviceOnlineMessage;
import org.jetlinks.protocol.message.event.EventMessage;
import org.jetlinks.protocol.message.function.FunctionInvokeMessageReply;
import org.jetlinks.protocol.message.property.ReadPropertyMessageReply;
import org.jetlinks.protocol.message.property.WritePropertyMessageReply;
import org.jetlinks.protocol.metadata.Jsonable;
import org.jetlinks.registry.api.DeviceMessageHandler;
import org.jetlinks.registry.api.DeviceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jetlinks.cloud.DeviceConfigKey.*;

/**
 * 处理来自设备的消息
 *
 * @author bsetfeng
 * @author zhouhao
 * @version 1.0
 **/
@Component
@Slf4j
@EnableBinding
public class FromDeviceMessageHandler {

    @Autowired
    private DeviceMessageHandler deviceMessageHandler;

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private DeviceSessionManager sessionManager;

    @Autowired
    private BinderAwareChannelResolver resolver;

    protected Object newConnectData(String deviceId) {
        JSONObject object = new JSONObject();
        object.put("deviceId", deviceId);
        object.put("timestamp", System.currentTimeMillis());
        return object;
    }

    @EventListener
    public void handleDeviceRegisterEvent(DeviceOnlineEvent registerEvent) {
        trySendMessageToMq(() -> newConnectData(registerEvent.getSession().getDeviceId()),
                deviceConnectTopic.getConfigValue(registerEvent.getSession()
                        .getOperation()).asList(String.class));
    }

    @EventListener
    public void handleDeviceUnRegisterEvent(DeviceOfflineEvent registerEvent) {
        trySendMessageToMq(() -> newConnectData(registerEvent.getSession().getDeviceId()),
                deviceDisconnectTopic.getConfigValue(registerEvent.getSession()
                        .getOperation()).asList(String.class));
    }

    @EventListener
    public void handleChildDeviceOnlineMessage(DeviceMessageEvent<ChildDeviceOnlineMessage> event) {
        ChildDeviceOnlineMessage message = event.getMessage();
        DeviceSession session = event.getSession();
        // TODO: 19-3-21 子设备认证

        DeviceOperation operation = registry.getDevice(message.getChildDeviceId());
        operation.online(sessionManager.getServerId(), session.getId());

        trySendMessageToMq(() -> new ChildDeviceOnlineEvent(session.getDeviceId(), message.getChildDeviceId(), System.currentTimeMillis()),
                childDeviceConnectTopic.getConfigValue(session.getOperation()).asList(String.class));

        trySendMessageToMq(() -> newConnectData(message.getChildDeviceId()),
                deviceConnectTopic.getConfigValue(session.getOperation()).asList(String.class));

    }

    @EventListener
    public void handleChildDeviceOfflineMessage(DeviceMessageEvent<ChildDeviceOfflineMessage> event) {
        ChildDeviceOfflineMessage message = event.getMessage();
        DeviceSession session = event.getSession();
        //子设备下线
        DeviceOperation operation = registry.getDevice(message.getChildDeviceId());
        operation.offline();

        trySendMessageToMq(() -> new ChildDeviceOfflineEvent(session.getDeviceId(), message.getChildDeviceId(), System.currentTimeMillis()),
                childDeviceConnectTopic.getConfigValue(session.getOperation()).asList(String.class));

        trySendMessageToMq(() -> newConnectData(message.getChildDeviceId()),
                deviceConnectTopic.getConfigValue(session.getOperation()).asList(String.class));

    }

    @EventListener
    public void handleFunctionReplyMessage(DeviceMessageEvent<FunctionInvokeMessageReply> event) {
//        FunctionInvokeMessageReply message = event.getMessage();
        DeviceSession session = event.getSession();
        // 设备配置了转发到指定的topic
        trySendMessageToMq(event::getMessage,
                functionReplyTopic.getConfigValue(session.getOperation()).asList(String.class));

    }

    @EventListener
    public void handleReadPropertyReplyMessage(DeviceMessageEvent<ReadPropertyMessageReply> event) {
        ReadPropertyMessageReply invokeMessage = event.getMessage();
        if (StringUtils.isEmpty(invokeMessage.getMessageId())) {
            log.warn("消息无messageId:{}", invokeMessage.toJson());
            return;
        }
        deviceMessageHandler.reply(invokeMessage);
    }

    @EventListener
    public void handleWritePropertyReplyMessage(DeviceMessageEvent<WritePropertyMessageReply> event) {
        WritePropertyMessageReply invokeMessage = event.getMessage();
        if (StringUtils.isEmpty(invokeMessage.getMessageId())) {
            log.warn("消息无messageId:{}", invokeMessage.toJson());
            return;
        }
        deviceMessageHandler.reply(invokeMessage);
    }

    @EventListener
    public void handleEventMessage(DeviceMessageEvent<EventMessage> event) {
        DeviceSession session = event.getSession();
        // 设备配置了转发到指定的topic
        trySendMessageToMq(event::getMessage,
                eventTopic.getConfigValue(session.getOperation(), event.getMessage().getEvent()).asList(String.class));

        // 设备配置了转发到指定的topic
        trySendMessageToMq(event::getMessage,
                eventTopic.getConfigValue(session.getOperation()).asList(String.class));
    }


    @SafeVarargs
    private final void trySendMessageToMq(Supplier<Object> messageSupplier, Optional<List<String>>... topic) {
        List<String> topics = Stream.of(topic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (!topics.isEmpty()) {
            Object message = messageSupplier.get();
            String json;
            if (message instanceof Jsonable) {
                json = ((Jsonable) message).toJson().toJSONString();
            } else if (message instanceof String) {
                json = ((String) message);
            } else {
                json = JSON.toJSONString(message);
            }
            sendMessageToMq(topics, json);
        }
    }


    private void sendMessageToMq(List<String> topics, String json) {
        log.info("发送消息到MQ,topics:{} <= {}", topics, json);
        for (String topic : topics) {
            resolver.resolveDestination(topic)
                    .send(MessageBuilder.withPayload(json)
                            .build());
        }
    }
}
