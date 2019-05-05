package org.jetlinks.cloud.device.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.commons.entity.RecordCreationEntity;
import org.hswebframework.web.commons.entity.SimpleTreeSortSupportEntity;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.List;

@Getter
@Setter
@Table(name = "dev_classified_catalog")
public class ClassifiedCatalogEntity extends SimpleTreeSortSupportEntity<String> implements RecordCreationEntity {

    //分类目录名称
    @Column(name = "name")
    private String name;

    //说明
    @Column(name = "describe")
    private String describe;

    //创建人
    @Column(name = "creator_id")
    private String creatorId;

    //创建时间
    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "state")
    private Byte state;

    @Column(name = "search_code")
    private String searchCode;

    @Override
    @Column(name = "parent_id")
    public String getParentId() {
        return super.getParentId();
    }

    @Override
    @Column(name = "_level")
    public Integer getLevel() {
        return super.getLevel();
    }

    @Override
    @Column(name = "path")
    public String getPath() {
        return super.getPath();
    }

    @Override
    @Column(name = "sort_index")
    public Long getSortIndex() {
        return super.getSortIndex();
    }


    private List<ClassifiedCatalogEntity> children;

}
