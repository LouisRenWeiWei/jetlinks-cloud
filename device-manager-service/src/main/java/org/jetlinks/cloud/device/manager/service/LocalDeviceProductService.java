package org.jetlinks.cloud.device.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.service.GenericEntityService;
import org.jetlinks.cloud.device.manager.dao.DeviceProductDao;
import org.jetlinks.cloud.device.manager.entity.DeviceProductEntity;
import org.jetlinks.cloud.device.manager.enums.DeviceFeature;
import org.jetlinks.core.device.DeviceProductInfo;
import org.jetlinks.core.device.DeviceProductOperation;
import org.jetlinks.registry.api.DeviceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static java.util.Optional.*;

@Service
@Slf4j
public class LocalDeviceProductService extends GenericEntityService<DeviceProductEntity, String> {

    @Autowired
    private DeviceProductDao deviceProductDao;

    @Autowired
    private DeviceRegistry registry;

    @Override
    protected IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    @Override
    public DeviceProductDao getDao() {
        return deviceProductDao;
    }

    @Override
    public String insert(DeviceProductEntity entity) {
        String id = super.insert(entity);
        updateRegistry(entity);
        return id;
    }

    @Override
    public int updateByPk(String id, DeviceProductEntity entity) {
        int len = super.updateByPk(id, entity);
        updateRegistry(entity);
        return len;
    }

    public void updateRegistry(DeviceProductEntity entity) {
        Runnable runnable = () -> {
            logger.info("update device product[{}:{}] registry info", entity.getId(), entity.getName());
            DeviceProductInfo productInfo = new DeviceProductInfo();
            productInfo.setId(entity.getId());
            productInfo.setName(entity.getName());
            productInfo.setProjectId(entity.getProjectId());
            productInfo.setProjectName(entity.getProjectName());
            productInfo.setProtocol(entity.getMessageProtocol());
            DeviceProductOperation operation = registry.getProduct(entity.getId());

            DeviceFeature.DEFAULT.doConfig(operation);
            entity.acceptFeature(feature -> feature.doConfig(operation));
            operation.update(productInfo);
            operation.updateMetadata(entity.getMetadata());
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
