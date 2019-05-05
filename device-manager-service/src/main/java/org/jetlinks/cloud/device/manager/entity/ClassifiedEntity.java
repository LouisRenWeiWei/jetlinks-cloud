package org.jetlinks.cloud.device.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.commons.entity.RecordCreationEntity;
import org.hswebframework.web.commons.entity.SimpleGenericEntity;

import javax.persistence.Column;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "dev_classified")
public class ClassifiedEntity extends SimpleGenericEntity<String> implements RecordCreationEntity {

    //分类名称
    @Column(name = "name")
    private String name;

    //分类id
    @Column(name = "catalog_id")
    private String catalogId;

    //创建人id
    @Column(name = "creator_id")
    private String creatorId;

    //创建时间
    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "state")
    private Byte state;

    @Column(name = "search_code")
    private String searchCode;
    
    @Column(name = "default_metadata")
    private String defaultMetadata;

}
