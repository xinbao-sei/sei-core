package com.changhong.sei.core.dto;

import java.io.Serializable;

/**
 * 实现功能: 树形实体移动参数
 *
 * @author 王锦光 wangjg
 * @version 2020-02-05 10:06
 */
public class TreeNodeMoveParam implements Serializable {
    /**
     * 要移动的节点Id
     */
    private String nodeId;

    /**
     * 目标父节点Id
     */
    private String targetParentId;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getTargetParentId() {
        return targetParentId;
    }

    public void setTargetParentId(String targetParentId) {
        this.targetParentId = targetParentId;
    }
}
