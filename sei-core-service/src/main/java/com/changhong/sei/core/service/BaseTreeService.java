package com.changhong.sei.core.service;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.context.SessionUser;
import com.changhong.sei.core.dao.BaseTreeDao;
import com.changhong.sei.core.dto.TreeEntity;
import com.changhong.sei.core.dto.auth.AuthTreeEntityData;
import com.changhong.sei.core.dto.auth.IDataAuthTreeEntity;
import com.changhong.sei.core.entity.BaseEntity;
import com.changhong.sei.core.service.bo.OperateResult;
import com.changhong.sei.core.service.bo.OperateResultWithData;
import com.changhong.sei.enums.UserAuthorityPolicy;
import com.changhong.sei.exception.ServiceException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <strong>实现功能:</strong>
 * <p>树形实体基础业务逻辑类</p>
 *
 * @author 王锦光 wangj
 * @version 1.0.1 2019-12-26 9:40
 */
public abstract class BaseTreeService<T extends BaseEntity & TreeEntity<T>> extends BaseService<T, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTreeService.class);

    @Override
    protected abstract BaseTreeDao<T> getDao();

    /**
     * 保存树对象
     * 通过entity.isNew()判断是否是新增，当为true时，执行创建操作；为fasle时，执行更新操作
     * 约束：不允许修改parentId
     *
     * @param entity 树形结构实体
     * @return 返回操作对象
     */
    @Override
    @Transactional
    public OperateResultWithData<T> save(T entity) {
        Validation.notNull(entity, "持久化对象不能为空");
        String parentId = entity.getParentId();
        OperateResultWithData<T> operateResultWithData;
        boolean isNew = isNew(entity);
        if (isNew) {
            operateResultWithData = preInsert(entity);
        } else {
            operateResultWithData = preUpdate(entity);
        }

        if (Objects.isNull(operateResultWithData) || operateResultWithData.successful()) {
            // setNodeLevel setCodePath setNamePath
            //父ID为空，则表示是树根节点
            if (StringUtils.isBlank(parentId)) {
                entity.setNodeLevel(0);
                entity.setCodePath(TreeEntity.CODE_DELIMITER + entity.getCode());
                entity.setNamePath(TreeEntity.NAME_DELIMITER + entity.getName());
            } else {
                //获取父节点
                TreeEntity<T> parentNode = findOne(parentId);
                if (Objects.nonNull(parentNode)) {
                    //设置层级
                    entity.setNodeLevel(Objects.nonNull(parentNode.getNodeLevel()) ? parentNode.getNodeLevel() + 1 : 0);

                    StringBuilder str = new StringBuilder(128);
                    //设置代码路径
                    str.append(parentNode.getCodePath()).append(TreeEntity.CODE_DELIMITER).append(entity.getCode());
                    entity.setCodePath(str.toString());
                    str.setLength(0);

                    //设置名称路径
                    str.append(parentNode.getNamePath()).append(TreeEntity.NAME_DELIMITER).append(entity.getName());
                    entity.setNamePath(str.toString());
                    str.setLength(0);
                } else {
                    // error
                    return OperateResultWithData.operationFailureWithData(entity, "core_service_00030");
                }
            }

            if (!isNew) {
                //通过当前ID获取原始数据
                String originId = entity.getId();
                T origin = findOne(originId);
                if (Objects.nonNull(origin)) {
                    //验证当前parentId是否与原parentId相同,用以控制不允许修改父节点操作
                    if (!StringUtils.equalsAny(origin.getParentId(), parentId, null, "")) {
                        return OperateResultWithData.operationFailureWithData(entity, "core_service_00031");
                    }
                } else {
                    //未找到原始数据
                    return OperateResultWithData.operationFailureWithData(entity, "core_service_00029");
                }

                //检查是否修改代码和名称，以便同步更新子节点的路径
                List<T> childrenList = getChildrenNodesNoneOwn(originId);
                if (CollectionUtils.isNotEmpty(childrenList)) {
                    String temp;
                    for (T item : childrenList) {
                        temp = entity.getCodePath() + item.getCodePath().substring(origin.getCodePath().length());
                        item.setCodePath(temp);
                        temp = entity.getNamePath() + item.getNamePath().substring(origin.getNamePath().length());
                        item.setNamePath(temp);

                        getDao().save(item);
                    }
                }
            }

            getDao().save(entity);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Saved entity id is {}", entity.getId());
            }

            if (isNew) {
                operateResultWithData = OperateResultWithData.operationSuccessWithData(entity, "core_service_00026");
            } else {
                operateResultWithData = OperateResultWithData.operationSuccessWithData(entity, "core_service_00027");
            }
        }
        return operateResultWithData;
    }

    /**
     * 批量保存多个树节点
     *
     * @param entities 待批量操作数据集合
     */
    @Override
    @Transactional
    public void save(Collection<T> entities) {
        OperateResultWithData<T> operateResult;
        if (CollectionUtils.isNotEmpty(entities)) {
            for (T entity : entities) {
                operateResult = save(entity);
                if (operateResult.notSuccessful()) {
                    throw new ServiceException(operateResult.getMessage());
                }
            }
        }
    }

    /**
     * 通过Id标识删除一个树节点
     *
     * @param id 主键
     * @return 操作结果
     */
    @Override
    @Transactional
    public OperateResult delete(String id) {
        OperateResult operateResult = preDelete(id);
        if (operateResult.successful()) {
            T entity = findOne(id);
            if (Objects.nonNull(entity)) {
                List<T> childrenList = getChildrenNodesNoneOwn(id);
                if (CollectionUtils.isEmpty(childrenList)) {
                    getDao().delete(entity);
                    return OperateResult.operationSuccess("core_service_00028");
                } else {
                    return OperateResult.operationFailure("core_service_00032");
                }
            } else {
                return OperateResult.operationWarning("core_service_00029");
            }
        } else {
            return operateResult;
        }
    }

    /**
     * 批量删除树节点
     *
     * @param ids 待批量操作数据集合
     */
    @Override
    @Transactional
    public void delete(Collection<String> ids) {
        List<T> list = findByIds(ids);
        if (CollectionUtils.isNotEmpty(list)) {
            OperateResult operateResult;
            for (T t : list) {
                operateResult = delete(t.getId());
                if (operateResult.notSuccessful()) {
                    throw new ServiceException(operateResult.getMessage());
                }
            }
        }
    }

    /**
     * 移动节点
     *
     * @param currentNodeId  当前节点ID
     * @param targetParentId 目标父节点ID
     * @return 返回操作结果对象
     */
    @Transactional
    public OperateResult move(String currentNodeId, String targetParentId) {
        if (StringUtils.isBlank(currentNodeId)) {
            return OperateResult.operationFailure("core_service_00033", "当前节点ID");
        }
        if (StringUtils.isBlank(targetParentId)) {
            return OperateResult.operationFailure("core_service_00033", "目标父节点ID");
        }
        // 检查不能是本节点
        if (StringUtils.equals(currentNodeId, targetParentId)) {
            // 移动时不能将父节点设置为本节点！
            return OperateResult.operationFailure("core_service_00040");
        }
        //获取当前节点
        T currentNode = findOne(currentNodeId);
        if (Objects.isNull(currentNode)) {
            return OperateResult.operationWarning("core_service_00029");
        }
        OperateResult operateResult;
        //当前节点的父节点
        T currentParent = null;
        if (StringUtils.isNotBlank(currentNode.getParentId())) {
            currentParent = findOne(currentNode.getParentId());
        }
        //移动目标父节点
        T targetParent = findOne(targetParentId);
        if (Objects.nonNull(targetParent)) {
            //检查当前父id与目标父id是否不同，如果相同不执行移动
            if (Objects.nonNull(currentParent) && StringUtils.equals(currentParent.getId(), targetParent.getId())) {
                return OperateResult.operationSuccess("core_service_00034");
            }
            int parentNodeLevel = 0;
            String parentCodePath = "";
            String parentNamePath = "";
            if (Objects.nonNull(currentParent)) {
                parentNodeLevel = currentParent.getNodeLevel();
                parentCodePath = currentParent.getCodePath();
                parentNamePath = currentParent.getNamePath();
            }

            //目标父层级 - 当前父层级
            int difference = targetParent.getNodeLevel() - parentNodeLevel;
            List<T> childrenList = findByCodePathStartingWith(currentNode.getCodePath());
            if (CollectionUtils.isNotEmpty(childrenList)) {
                String temp;
                for (T item : childrenList) {
                    //是否是当前节点
                    if (currentNodeId.equals(item.getId())) {
                        item.setParentId(targetParentId);
                    }

                    if (Objects.nonNull(currentParent)) {
                        item.setNodeLevel(item.getNodeLevel() + difference);
                        temp = targetParent.getCodePath() + item.getCodePath().substring(parentCodePath.length());
                        item.setCodePath(temp);
                        temp = targetParent.getNamePath() + item.getNamePath().substring(parentNamePath.length());
                        item.setNamePath(temp);
                    } else {
                        item.setNodeLevel(item.getNodeLevel() + difference + 1);
                        temp = targetParent.getCodePath() + item.getCodePath();
                        item.setCodePath(temp);
                        temp = targetParent.getNamePath() + item.getNamePath();
                        item.setNamePath(temp);
                    }
                    getDao().save(item);
                }
            }
            operateResult = OperateResult.operationSuccess("core_service_00034");
        } else {
            operateResult = OperateResult.operationWarning("core_service_00029");
        }
        return operateResult;
    }

    /**
     * 获取所有树根节点
     *
     * @return 返回树根节点集合
     */
    public List<T> getAllRootNode() {
        return getDao().getAllRootNode();
    }

    /**
     * 获取一个节点的所有子节点
     *
     * @param nodeId      节点Id
     * @param includeSelf 是否包含本节点
     * @return 子节点清单
     */
    public List<T> getChildrenNodes(String nodeId, boolean includeSelf) {
        if (includeSelf) {
            return getDao().getChildrenNodes(nodeId);
        }
        return getDao().getChildrenNodesNoneOwn(nodeId);
    }

    /**
     * 获取指定节点下的所有子节点(包含自己)
     *
     * @param nodeId 当前节点ID
     * @return 返回指定节点下的所有子节点(包含自己)
     */
    public List<T> getChildrenNodes(String nodeId) {
        return getDao().getChildrenNodes(nodeId);
    }

    /**
     * 获取指定节点下的所有子节点(不包含自己)
     *
     * @param nodeId 当前节点ID
     * @return 返回指定节点下的所有子节点(不包含自己)
     */
    public List<T> getChildrenNodesNoneOwn(String nodeId) {
        return getDao().getChildrenNodesNoneOwn(nodeId);
    }

    /**
     * 获取指定节点名称的所有节点
     *
     * @param nodeName 当前节点名称
     * @return 返回指定节点名称的所有节点
     */
    public List<T> getChildrenNodesByName(String nodeName) {
        return getDao().getChildrenNodesByName(nodeName);
    }

    /**
     * 获取树
     *
     * @param nodeId 当前节点ID
     * @return 返回指定节点树形对象
     */
    public T getTree(String nodeId) {
        return getDao().getTree(nodeId);
    }

    /**
     * 通过代码路径获取指定路径开头的集合
     *
     * @param codePath 代码路径
     * @return 返回指定代码路径开头的集合
     */
    public List<T> findByCodePathStartingWith(String codePath) {
        return getDao().findByCodePathStartingWith(codePath);
    }

    /**
     * 获取指定代码路径下的所有子节点(不包含自己)
     *
     * @param codePath 代码路径
     * @param nodeId   本节点Id
     * @return 子节点
     */
    public List<T> findByCodePathStartingWithAndIdNot(String codePath, String nodeId) {
        return getDao().findByCodePathStartingWithAndIdNot(codePath, nodeId);
    }

    /**
     * 通过名称路径获取指定路径开头的集合
     *
     * @param namePath 名称路径
     * @return 返回指定名称路径开头的集合
     */
    public List<T> findByNamePathStartingWith(String namePath) {
        return getDao().findByNamePathStartingWith(namePath);
    }

    /**
     * 获取指定名称路径下的所有子节点(不包含自己)
     *
     * @param namePath 名称路径
     * @param nodeId   本节点Id
     * @return 子节点
     */
    public List<T> findByNamePathStartingWithAndIdNot(String namePath, String nodeId) {
        return getDao().findByNamePathStartingWithAndIdNot(namePath, nodeId);
    }

    /**
     * 节点名称模糊获取节点
     *
     * @param nodeName 节点名称
     * @return 返回含有指定节点名称的集合
     */
    public List<T> findByNamePathLike(String nodeName) {
        return getDao().findByNamePathLike(nodeName);
    }

    /**
     * 获取一个节点的所有父节点
     *
     * @param node        节点
     * @param includeSelf 返回值中是否包含节点本身
     * @return 父节点清单
     */
    public List<T> getParentNodes(T node, boolean includeSelf) {
        List<T> parents = new LinkedList<>();
        if (node == null) {
            return parents;
        }
        if (includeSelf) {
            parents.add(node);
        }
        T parent = getParent(node);
        while (parent != null) {
            parents.add(parent);
            parent = getParent(parent);
        }
        return parents;
    }

    /**
     * 获取一个节点的父节点
     *
     * @param node 节点
     * @return 父节点
     */
    private T getParent(T node) {
        if (node.getParentId() == null) {
            return null;
        }
        //获取父节点
        return findOne(node.getParentId());
    }

    /**
     * 获取一个节点的所有父节点
     *
     * @param nodeId      节点Id
     * @param includeSelf 返回值中是否包含节点本身
     * @return 父节点清单
     */
    public List<T> getParentNodes(String nodeId, boolean includeSelf) {
        T node = findOne(nodeId);
        return getParentNodes(node, includeSelf);
    }

    /**
     * 递归查找子节点并设置子节点
     *
     * @param treeNode 树形节点（顶级节点）
     * @param nodes    节点清单
     * @return 树形节点
     */
    private static <Tree extends TreeEntity<Tree>> Tree findChildren(Tree treeNode, List<Tree> nodes) {
        for (Tree node : nodes) {
            if (treeNode.getId().equals(node.getParentId())) {
                if (treeNode.getChildren() == null) {
                    treeNode.setChildren(new ArrayList<>());
                }
                treeNode.getChildren().add(findChildren(node, nodes));
            }
        }
        return treeNode;
    }

    /**
     * 通过节点清单构建树
     *
     * @param nodes 节点清单
     * @return 树
     */
    public static <Tree extends TreeEntity<Tree>> List<Tree> buildTree(List<Tree> nodes) {
        List<Tree> result = new ArrayList<>();
        if (nodes == null || nodes.size() == 0) {
            return result;
        }
        //将输入节点排序
        List<Tree> sordedNodes = nodes.stream().sorted(Comparator.comparingInt(n -> n.getNodeLevel() + n.getRank())).collect(Collectors.toList());
        //获取清单中的顶级节点
        for (Tree node : sordedNodes) {
            String parentId = node.getParentId();
            Tree parent = sordedNodes.stream().filter((n) -> StringUtils.equals(n.getId(), parentId)
                    && !StringUtils.equals(n.getId(), node.getId())).findAny().orElse(null);
            if (parent == null) {
                //递归构造子节点
                findChildren(node, sordedNodes);
                result.add(node);
            }
        }
        return result.stream().sorted(Comparator.comparingInt(Tree::getRank)).collect(Collectors.toList());
    }

    /**
     * 递归获取所有子节点清单(包含自己)
     *
     * @param treeNode 树形节点（顶级节点）
     * @param nodes    子节点清单
     */
    public static <Tree extends TreeEntity<Tree>> void getAllChildren(Tree treeNode, List<Tree> nodes) {
        nodes.add(treeNode);
        if (CollectionUtils.isNotEmpty(treeNode.getChildren())) {
            List<Tree> children = treeNode.getChildren();
            // 清除节点的子节点清单
            treeNode.setChildren(null);
            nodes.addAll(children);
            children.forEach(c -> getAllChildren(c, nodes));
        }
    }

    /**
     * 将树反构建为节点清单
     *
     * @param trees 树
     * @return 节点清单
     */
    public static <Tree extends TreeEntity<Tree>> List<Tree> unBuildTree(List<Tree> trees) {
        Set<Tree> nodeSet = new LinkedHashSet<>();
        trees.forEach(tree -> {
            List<Tree> children = new ArrayList<>();
            getAllChildren(tree, children);
            nodeSet.addAll(children);
        });
        return new ArrayList<>(nodeSet);
    }

    /**
     * 将树反构建为节点Id清单
     *
     * @param trees 树
     * @return 节点清单
     */
    public static <Tree extends TreeEntity<Tree>> List<String> unBuildTreeIds(List<Tree> trees) {
        Set<String> nodeSet = new LinkedHashSet<>();
        trees.forEach(tree -> {
            List<String> childIds = new ArrayList<>();
            getAllChildIds(tree, childIds);
            nodeSet.addAll(childIds);
        });
        return new ArrayList<>(nodeSet);
    }

    /**
     * 递归获取所有子节点Id清单(包含自己)
     *
     * @param treeNode 树形节点（顶级节点）
     * @param childIds 子节点Id清单
     */
    public static <Tree extends TreeEntity<Tree>> void getAllChildIds(Tree treeNode, List<String> childIds) {
        childIds.add(treeNode.getId());
        if (CollectionUtils.isNotEmpty(treeNode.getChildren())) {
            List<Tree> children = treeNode.getChildren();
            childIds.addAll(children.stream().map(TreeEntity::getId).collect(Collectors.toList()));
            children.forEach(c -> getAllChildIds(c, childIds));
        }
    }

    /**
     * 通过业务实体Id清单获取数据权限树形实体清单
     *
     * @param ids 业务实体Id清单
     * @return 数据权限树形实体清单
     */
    public List<AuthTreeEntityData> getAuthTreeEntityDataByIds(List<String> ids) {
        Class<T> entityClass = getDao().getEntityClass();
        //判断是否实现数据权限业务实体接口
        if (!IDataAuthTreeEntity.class.isAssignableFrom(entityClass)) {
            return Collections.emptyList();
        }
        //获取所有未冻结的业务实体
        List<T> allEntities = getDao().findAllUnfrozen();
        if (allEntities == null || allEntities.isEmpty()) {
            return Collections.emptyList();
        }
        //获取Id清单的业务实体
        List<T> entities = allEntities.stream().filter((p) -> ids.contains(p.getId())).collect(Collectors.toList());
        List<AuthTreeEntityData> dataList = new ArrayList<>();
        entities.forEach((p) -> dataList.add(new AuthTreeEntityData((IDataAuthTreeEntity) p)));
        //装配成树形结构
        return buildTree(dataList);
    }

    /**
     * 获取所有数据权限树形实体清单
     *
     * @return 数据权限树形实体清单
     */
    public List<AuthTreeEntityData> findAllAuthTreeEntityData() {
        Class<T> entityClass = getDao().getEntityClass();
        //判断是否实现数据权限业务实体接口
        if (!IDataAuthTreeEntity.class.isAssignableFrom(entityClass)) {
            return Collections.emptyList();
        }
        //获取所有未冻结的业务实体
        List<T> allEntities = getDao().findAllUnfrozen();
        if (allEntities == null || allEntities.isEmpty()) {
            return Collections.emptyList();
        }
        List<AuthTreeEntityData> dataList = new ArrayList<>();
        allEntities.forEach((p) -> dataList.add(new AuthTreeEntityData((IDataAuthTreeEntity) p)));
        //装配成树形结构
        return buildTree(dataList);
    }

    /**
     * 获取当前用户有权限的树形业务实体清单
     *
     * @param featureCode   功能项代码
     * @param includeFrozen 是否包含冻结的实体
     * @return 有权限的树形业务实体清单
     */
    public List<T> getUserAuthorizedTreeEntities(String featureCode, Boolean includeFrozen) {
        Class<T> entityClass = getDao().getEntityClass();
        //判断是否实现数据权限业务实体接口
        if (!IDataAuthTreeEntity.class.isAssignableFrom(entityClass)) {
            return Collections.emptyList();
        }
        //获取当前用户
        SessionUser sessionUser = ContextUtil.getSessionUser();
        //如果是匿名用户无数据
        if (sessionUser.isAnonymous()) {
            return Collections.emptyList();
        }
        List<T> resultList;
        List<T> allEntities;
        UserAuthorityPolicy authorityPolicy = sessionUser.getAuthorityPolicy();
        switch (authorityPolicy) {
            case GlobalAdmin:
                //如果是全局管理，无数据
                resultList = Collections.emptyList();
                break;
            case TenantAdmin:
                //如果是租户管理员，返回租户的所有数据(所有/未冻结)
                if (Objects.nonNull(includeFrozen) && includeFrozen) {
                    allEntities = getDao().findAll();
                } else {
                    allEntities = getDao().findAllUnfrozen();
                }
                if (allEntities == null || allEntities.isEmpty()) {
                    resultList = Collections.emptyList();
                } else {
                    resultList = buildTree(allEntities);
                }
                break;
            case NormalUser:
            default:
                //如果是一般用户，先获取有权限的角色对应的业务实体Id清单
                List<String> entityIds = getNormalUserAuthorizedEntityIds(featureCode, sessionUser.getUserId());
                if (entityIds == null || entityIds.isEmpty()) {
                    resultList = Collections.emptyList();
                } else {
                    //先获取所有未冻结的业务实体
                    if (Objects.nonNull(includeFrozen) && includeFrozen) {
                        allEntities = getDao().findAll();
                    } else {
                        allEntities = getDao().findAllUnfrozen();
                    }
                    if (allEntities == null || allEntities.isEmpty()) {
                        resultList = Collections.emptyList();
                    } else {
                        List<T> entities = allEntities.stream().filter((p) -> entityIds.contains(p.getId())).collect(Collectors.toList());
                        resultList = buildTree(entities);
                    }
                }
                break;
        }
        return resultList;
    }

    /**
     * 获取当前用户有权限的树形业务实体清单（不含冻结）
     *
     * @param featureCode 功能项代码
     * @return 有权限的树形业务实体清单
     */
    public List<T> getUserAuthorizedTreeEntities(String featureCode) {
        return getUserAuthorizedTreeEntities(featureCode, false);
    }

    /**
     * 获取当前用户有权限的树形节点代码清单(包含冻结)
     *
     * @param featureCode 功能项代码
     * @return 节点代码清单
     */
    public List<String> getUserAuthorizedTreeNodeCodes(String featureCode) {
        List<T> entities = getUserAuthorizedTreeEntities(featureCode, true);
        if (CollectionUtils.isEmpty(entities)) {
            return new LinkedList<>();
        }
        Set<String> codeSet = new LinkedHashSet<>();
        List<T> nodes = new LinkedList<>();
        entities.forEach(tree -> {
            // 先添加自己
            nodes.add(tree);
            // 获取树的所有节点
            fetchChildrenFromTree(tree, nodes);
        });
        // 追加节点代码
        nodes.forEach(node -> codeSet.add(node.getCode()));
        return new LinkedList<>(codeSet);
    }

    /**
     * 递归获取并设置一个树形结构的所有节点
     *
     * @param treeNode 树形结构节点
     * @param nodes    所有节点
     */
    private void fetchChildrenFromTree(T treeNode, List<T> nodes) {
        if (Objects.isNull(treeNode) || CollectionUtils.isEmpty(treeNode.getChildren())) {
            return;
        }
        List<T> children = treeNode.getChildren();
        nodes.addAll(children);
        children.forEach(node -> fetchChildrenFromTree(node, nodes));
    }
}
