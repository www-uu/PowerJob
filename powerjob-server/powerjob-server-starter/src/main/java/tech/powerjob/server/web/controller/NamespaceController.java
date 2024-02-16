package tech.powerjob.server.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.plugin.ModifyOrCreateDynamicPermission;
import tech.powerjob.server.auth.plugin.SaveNamespaceGrantPermissionPlugin;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.request.QueryNamespaceRequest;
import tech.powerjob.server.web.response.NamespaceBaseVO;
import tech.powerjob.server.web.response.NamespaceVO;
import tech.powerjob.server.web.service.NamespaceWebService;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 命名空间 Controller
 *
 * @author tjq
 * @since 2023/9/3
 */
@Slf4j
@RestController
@RequestMapping("/namespace")
public class NamespaceController {

    @Resource
    private WebAuthService webAuthService;
    @Resource
    private NamespaceWebService namespaceWebService;

    @ResponseBody
    @PostMapping("/save")
    @ApiPermission(name = "Namespace-Save", roleScope = RoleScope.NAMESPACE, dynamicPermissionPlugin = ModifyOrCreateDynamicPermission.class, grandPermissionPlugin = SaveNamespaceGrantPermissionPlugin.class)
    public ResultDTO<NamespaceVO> save(@RequestBody ModifyNamespaceRequest req) {

        NamespaceDO savedNamespace = namespaceWebService.save(req);
        return ResultDTO.success(NamespaceConverter.do2BaseVo(savedNamespace));
    }

    @DeleteMapping("/delete")
    @ApiPermission(name = "Namespace-Delete", roleScope = RoleScope.NAMESPACE, requiredPermission = Permission.SU)
    public ResultDTO<Void> deleteNamespace(Long id) {
        namespaceWebService.delete(id);
        return ResultDTO.success(null);
    }

    @PostMapping("/list")
    @ApiPermission(name = "Namespace-List", roleScope = RoleScope.NAMESPACE, requiredPermission = Permission.NONE)
    public ResultDTO<PageResult<NamespaceVO>> listNamespace(@RequestBody QueryNamespaceRequest queryNamespaceRequest) {

        Page<NamespaceDO> namespacePageResult = namespaceWebService.list(queryNamespaceRequest);

        PageResult<NamespaceVO> ret = new PageResult<>(namespacePageResult);
        ret.setData(namespacePageResult.get().map(x -> {
            NamespaceVO namespaceVO = NamespaceConverter.do2BaseVo(x);
            fillPermissionInfo(x, namespaceVO);
            return namespaceVO;
        }).collect(Collectors.toList()));

        return ResultDTO.success(ret);
    }

    @PostMapping("/listAll")
    @ApiPermission(name = "Namespace-ListAll", roleScope = RoleScope.NAMESPACE, requiredPermission = Permission.NONE)
    public ResultDTO<List<NamespaceBaseVO>> listAll() {
        // 数量应该不是很多，先简单处理，不查询精简对象
        List<NamespaceDO> namespaceRepositoryAll = namespaceWebService.listAll();
        List<NamespaceBaseVO> namespaceBaseVOList = namespaceRepositoryAll.stream().map(nd -> {
            NamespaceBaseVO nv = new NamespaceBaseVO();
            nv.setId(nd.getId());
            nv.setCode(nd.getCode());
            nv.setName(nd.getName());
            nv.genFrontName();
            return nv;
        }).collect(Collectors.toList());
        return ResultDTO.success(namespaceBaseVOList);
    }

    private void fillPermissionInfo(NamespaceDO namespaceDO, NamespaceVO namespaceVO) {

        Long namespaceId = namespaceVO.getId();

        // 权限用户关系
        ComponentUserRoleInfo componentUserRoleInfo = webAuthService.fetchComponentUserRoleInfo(RoleScope.NAMESPACE, namespaceId);
        namespaceVO.setComponentUserRoleInfo(componentUserRoleInfo);

        // 有权限用户填充 token
        boolean hasPermission = webAuthService.hasPermission(RoleScope.NAMESPACE, namespaceId, Permission.READ);
        namespaceVO.setToken(hasPermission ? namespaceDO.getToken() : AuthConstants.TIPS_NO_PERMISSION_TO_SEE);
    }

}