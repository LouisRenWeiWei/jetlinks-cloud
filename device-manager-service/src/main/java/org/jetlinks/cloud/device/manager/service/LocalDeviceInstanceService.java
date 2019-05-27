package org.jetlinks.cloud.device.manager.service;

import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.service.GenericEntityService;
import org.jetlinks.cloud.device.manager.dao.DeviceInstanceDao;
import org.jetlinks.cloud.device.manager.entity.DeviceInstanceEntity;
import org.jetlinks.cloud.device.manager.entity.DeviceProductEntity;
import org.jetlinks.core.device.*;
import org.jetlinks.registry.api.DeviceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static java.util.Optional.ofNullable;

@Service
public class LocalDeviceInstanceService extends GenericEntityService<DeviceInstanceEntity, String> {

    @Autowired
    private DeviceInstanceDao deviceInstanceDao;

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private LocalDeviceProductService productService;

    @Override
    protected IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    @Override
    public DeviceInstanceDao getDao() {
        return deviceInstanceDao;
    }

    @Override
    public String insert(DeviceInstanceEntity entity) {
        entity.setState(DeviceState.noActive);
        String id = super.insert(entity);
        updateRegistry(entity);
        return id;
    }

    @Override
    public int updateByPk(String id, DeviceInstanceEntity entity) {
        int len = super.updateByPk(id, entity);
        updateRegistry(entity);
        return len;
    }

    /**
     * 同步设备状态
     *
     * @param deviceId 设备id集合
     * @param force    是否强制同步,将会检查设备的真实状态
     * @return 同步成功数量
     */
    public int syncState(List<String> deviceId, boolean force) {
        int total = 0;
        try {
            Map<Byte, Set<String>> group = new HashMap<>();
            for (String device : deviceId) {
                DeviceOperation operation = registry.getDevice(device);
                if (force) {
                    //检查真实状态
                    operation.checkState();
                }
                group.computeIfAbsent(operation.getState(), k -> new HashSet<>())
                        .add(device);
            }
            if (logger.isDebugEnabled()) {
                for (Map.Entry<Byte, Set<String>> entry : group.entrySet()) {
                    logger.debug("同步设备状态:{} 数量: {}", entry.getKey(), entry.getValue().size());
                }
            }

            //批量更新分组结果
            for (Map.Entry<Byte, Set<String>> entry : group.entrySet()) {
                byte state = entry.getKey();
                Set<String> deviceIdList = entry.getValue();
                if (CollectionUtils.isEmpty(deviceIdList)) {
                    continue;
                }
                total += createUpdate()
                        .set(DeviceInstanceEntity::getState, state)
                        .where()
                        .in(DeviceInstanceEntity::getId, deviceIdList)
                        .exec();
            }
        } catch (Exception e) {
            logger.error("同步设备状态失败:\n{}", deviceId);
        }
        return total;
    }

    public void updateRegistry(DeviceInstanceEntity entity) {
        Runnable runnable = () -> {
            DeviceProductEntity productEntity = productService.selectByPk(entity.getProductId());

            logger.info("update device instance[{}:{}] registry info", entity.getId(), entity.getName());
            DeviceInfo productInfo = new DeviceInfo();
            productInfo.setId(entity.getId());
            if (null != productEntity) {
                productInfo.setProductId(productEntity.getId());
                productInfo.setProductName(productEntity.getName());
                productInfo.setProtocol(productEntity.getMessageProtocol());
            }

            productInfo.setName(entity.getName());
            productInfo.setCreatorId(entity.getCreatorId());
            productInfo.setCreatorName(entity.getCreatorName());
            productInfo.setProjectId(entity.getProductId());
            productInfo.setProjectName(entity.getProdcutName());
            DeviceOperation operation = registry.getDevice(entity.getId());
            operation.update(productInfo);

            Optional.ofNullable(entity.getDeriveMetadata())
                    .ifPresent(operation::updateMetadata);

            if (operation.getState() == DeviceState.unknown) {
                operation.putState(DeviceState.noActive);
            }
            //自定义配置
            entity.acceptFeature(feature -> feature.doConfig(operation));

            ofNullable(entity.getSysConfiguration())
                    .ifPresent(operation::putAll);

            ofNullable(entity.getSecurity())
                    .ifPresent(operation::putAll);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
