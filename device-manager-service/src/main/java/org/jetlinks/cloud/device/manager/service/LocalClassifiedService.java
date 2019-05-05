package org.jetlinks.cloud.device.manager.service;

import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.service.GenericEntityService;
import org.jetlinks.cloud.device.manager.dao.ClassifiedDao;
import org.jetlinks.cloud.device.manager.entity.ClassifiedEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocalClassifiedService extends GenericEntityService<ClassifiedEntity,String> {

    @Autowired
    private ClassifiedDao classifiedDao;

    @Override
    protected IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    @Override
    public ClassifiedDao getDao() {
        return classifiedDao;
    }
}
