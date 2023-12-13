package com.youdeyiwu.service.user.impl;

import com.youdeyiwu.exception.CustomException;
import com.youdeyiwu.exception.PermissionNotFoundException;
import com.youdeyiwu.exception.RoleNotFoundException;
import com.youdeyiwu.mapper.user.PermissionMapper;
import com.youdeyiwu.mapper.user.RoleMapper;
import com.youdeyiwu.model.dto.user.AssignPermissionsDto;
import com.youdeyiwu.model.dto.user.CreateRoleDto;
import com.youdeyiwu.model.dto.user.UpdatePermissionsRoleDto;
import com.youdeyiwu.model.dto.user.UpdateRoleDto;
import com.youdeyiwu.model.entity.user.PermissionEntity;
import com.youdeyiwu.model.entity.user.RoleEntity;
import com.youdeyiwu.model.vo.PageVo;
import com.youdeyiwu.model.vo.user.PermissionEntityVo;
import com.youdeyiwu.model.vo.user.RoleEntityVo;
import com.youdeyiwu.model.vo.user.RolePermissionsVo;
import com.youdeyiwu.repository.user.PermissionRepository;
import com.youdeyiwu.repository.user.RoleRepository;
import com.youdeyiwu.service.user.SubmenuService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * submenu.
 *
 * @author dafengzhen
 */
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class SubmenuServiceImpl implements SubmenuService {

  private final RoleMapper roleMapper;

  private final RoleRepository roleRepository;

  private final PermissionRepository permissionRepository;

  private final PermissionMapper permissionMapper;

  @Transactional
  @Override
  public RoleEntity create(CreateRoleDto dto) {
    RoleEntity roleEntity = new RoleEntity();
    roleMapper.dtoToEntity(dto, roleEntity);
    roleRepository.save(roleEntity);
    return roleEntity;
  }

  @Transactional
  @Override
  public void addPermissions(Long id, AssignPermissionsDto dto) {
    RoleEntity roleEntity = roleRepository.findById(id)
        .orElseThrow(RoleNotFoundException::new);

    List<PermissionEntity> permissions = dto.ids().stream()
        .map(pid -> permissionRepository.findById(pid)
            .orElseThrow(PermissionNotFoundException::new)
        )
        .toList();

    roleEntity.getPermissions().addAll(permissions);
  }

  @Transactional
  @Override
  public void removePermissions(Long id, AssignPermissionsDto dto) {
    RoleEntity roleEntity = roleRepository.findById(id)
        .orElseThrow(RoleNotFoundException::new);

    List<PermissionEntity> permissions = dto.ids().stream()
        .map(pid -> permissionRepository.findById(pid)
            .orElseThrow(PermissionNotFoundException::new)
        )
        .toList();

    permissions.forEach(roleEntity.getPermissions()::remove);
  }

  @Transactional
  @Override
  public void updatePermissions(Long id, UpdatePermissionsRoleDto dto) {
    RoleEntity roleEntity = roleRepository.findById(id)
        .orElseThrow(RoleNotFoundException::new);

    if (Objects.nonNull(dto.permissions())) {
      roleEntity.setPermissions(
          dto.permissions()
              .stream()
              .map(pid -> permissionRepository.findById(pid)
                  .orElseThrow(PermissionNotFoundException::new)
              )
              .collect(Collectors.toSet())
      );
    }
  }

  @Transactional
  @Override
  public void update(Long id, UpdateRoleDto dto) {
    RoleEntity roleEntity = roleRepository.findById(id)
        .orElseThrow(RoleNotFoundException::new);
    roleMapper.dtoToEntity(dto, roleEntity);
  }

  @Override
  public RolePermissionsVo getPermissions(Long id) {
    RoleEntity roleEntity = roleRepository.findById(id)
        .orElseThrow(() -> new CustomException("The role does not exist"));

    List<PermissionEntityVo> permissions =
        roleEntity.getPermissions().stream()
            .map(permissionMapper::entityToVo)
            .toList();

    RolePermissionsVo vo = new RolePermissionsVo();
    vo.setRole(roleMapper.entityToVo(roleEntity));
    vo.setPermissions(permissions);
    return vo;
  }

  @Override
  public RoleEntityVo query(Long id) {
    RoleEntity roleEntity = roleRepository.findById(id)
        .orElseThrow(RoleNotFoundException::new);

    RoleEntityVo vo = roleMapper.entityToVo(roleEntity);
    setPermissions(vo, roleEntity);
    return vo;
  }

  @Override
  public PageVo<RoleEntityVo> queryAll(Pageable pageable) {
    return new PageVo<>(roleRepository.findAll(pageable).map(roleEntity -> {
      RoleEntityVo vo = roleMapper.entityToVo(roleEntity);
      setPermissions(vo, roleEntity);
      return vo;
    }));
  }

  @Transactional
  @Override
  public void delete(Long id) {
    RoleEntity roleEntity = roleRepository.findById(id)
        .orElseThrow(RoleNotFoundException::new);
    roleRepository.delete(roleEntity);
  }

  /**
   * set permissions.
   *
   * @param vo         vo
   * @param roleEntity roleEntity
   */
  private void setPermissions(RoleEntityVo vo, RoleEntity roleEntity) {
    vo.setPermissions(
        roleEntity.getPermissions()
            .stream()
            .map(permissionMapper::entityToVo)
            .collect(Collectors.toSet())
    );
  }
}