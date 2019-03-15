package com.aha.tech.core.model.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @Author: luweihong
 * @Date: 2019/2/22
 */
public class PairEntity<E> {

    private E firstEntity;
    private E secondEntity;

    public PairEntity(E firstEntity, E secondEntity){
        this.firstEntity = firstEntity;
        this.secondEntity = secondEntity;
    }

    public E getFirstEntity() {
        return firstEntity;
    }

    public void setFirstEntity(E firstEntity) {
        this.firstEntity = firstEntity;
    }

    public E getSecondEntity() {
        return secondEntity;
    }

    public void setSecondEntity(E secondEntity) {
        this.secondEntity = secondEntity;
    }

    @Override
    public String toString(){
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
