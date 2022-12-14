package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.stucms.api.student.StudentApi;
import cn.iocoder.yudao.module.stucms.api.student.dto.StudentRespDTO;
import cn.iocoder.yudao.module.stucms.api.teacher.TeacherApi;
import cn.iocoder.yudao.module.stucms.api.teacher.dto.TeacherRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.*;
import cn.iocoder.yudao.module.system.convert.user.UserConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * ???????????? Service ?????????
 *
 * @author ????????????
 */
@Service("adminUserService")
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    @Value("${sys.user.init-password:123456}")
    private String userInitPassword;

    @Resource
    private AdminUserMapper userMapper;

    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    @Lazy // ?????????????????????????????????
    private TenantService tenantService;

    @Resource
    private UserPostMapper userPostMapper;

    @Resource
    private FileApi fileApi;
    @Resource
    private StudentApi studentApi;
    @Resource
    private TeacherApi teacherApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReqVO reqVO) {
        // ??????????????????
        this.tenantService.handleTenantInfo(tenant -> {
            long count = this.userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // ???????????????
        this.checkCreateOrUpdate(null, reqVO.getUsername(), reqVO.getMobile(), reqVO.getEmail(),
            reqVO.getDeptId(), reqVO.getPostIds());
        // ????????????
        AdminUserDO user = UserConvert.INSTANCE.convert(reqVO);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // ????????????
        user.setPassword(this.encodePassword(reqVO.getPassword())); // ????????????
        this.userMapper.insert(user);
        // ??????????????????
        if (CollectionUtil.isNotEmpty(user.getPostIds())) {
            this.userPostMapper.insertBatch(convertList(user.getPostIds(),
                postId -> new UserPostDO().setUserId(user.getId()).setPostId(postId)));
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateReqVO reqVO) {
        // ???????????????
        this.checkCreateOrUpdate(reqVO.getId(), reqVO.getUsername(), reqVO.getMobile(), reqVO.getEmail(),
            reqVO.getDeptId(), reqVO.getPostIds());
        // ????????????
        AdminUserDO updateObj = UserConvert.INSTANCE.convert(reqVO);
        this.userMapper.updateById(updateObj);
        // ????????????
        this.updateUserPost(reqVO, updateObj);
    }

    private void updateUserPost(UserUpdateReqVO reqVO, AdminUserDO updateObj) {
        Long userId = reqVO.getId();
        Set<Long> dbPostIds = convertSet(this.userPostMapper.selectListByUserId(userId), UserPostDO::getPostId);
        // ????????????????????????????????????
        Set<Long> postIds = updateObj.getPostIds();
        Collection<Long> createPostIds = CollUtil.subtract(postIds, dbPostIds);
        Collection<Long> deletePostIds = CollUtil.subtract(dbPostIds, postIds);
        // ???????????????????????????????????????????????????????????????????????????
        if (!CollectionUtil.isEmpty(createPostIds)) {
            this.userPostMapper.insertBatch(convertList(createPostIds,
                postId -> new UserPostDO().setUserId(userId).setPostId(postId)));
        }
        if (!CollectionUtil.isEmpty(deletePostIds)) {
            this.userPostMapper.deleteByUserIdAndPostId(userId, deletePostIds);
        }
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        this.userMapper.updateById(new AdminUserDO().setId(id).setLoginIp(loginIp).setLoginDate(new Date()));
    }

    @Override
    public void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO) {
        // ???????????????
        this.checkUserExists(id);
        this.checkEmailUnique(id, reqVO.getEmail());
        this.checkMobileUnique(id, reqVO.getMobile());
        // ????????????
        this.userMapper.updateById(UserConvert.INSTANCE.convert(reqVO).setId(id));
    }

    @Override
    public void updateUserPassword(Long id, UserProfileUpdatePasswordReqVO reqVO) {
        // ?????????????????????
        this.checkOldPassword(id, reqVO.getOldPassword());
        // ????????????
        AdminUserDO updateObj = new AdminUserDO().setId(id);
        updateObj.setPassword(this.encodePassword(reqVO.getNewPassword())); // ????????????
        this.userMapper.updateById(updateObj);
    }

    @Override
    public String updateUserAvatar(Long id, InputStream avatarFile) throws Exception {
        this.checkUserExists(id);
        // ????????????
        String avatar = this.fileApi.createFile(IoUtil.readBytes(avatarFile));
        // ????????????
        AdminUserDO sysUserDO = new AdminUserDO();
        sysUserDO.setId(id);
        sysUserDO.setAvatar(avatar);
        this.userMapper.updateById(sysUserDO);
        return avatar;
    }

    @Override
    public void updateUserPassword(Long id, String password) {
        // ??????????????????
        this.checkUserExists(id);
        // ????????????
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setPassword(this.encodePassword(password)); // ????????????
        this.userMapper.updateById(updateObj);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        // ??????????????????
        this.checkUserExists(id);
        // ????????????
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        this.userMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        // ??????????????????
        this.checkUserExists(id);
        // ????????????
        this.userMapper.deleteById(id);
        // ????????????????????????
        this.permissionService.processUserDeleted(id);
        // ??????????????????
        this.userPostMapper.deleteByUserId(id);
    }

    @Override
    public AdminUserDO getUserByUsername(String username) {
        return this.userMapper.selectByUsername(username);
    }

    @Override
    public AdminUserDO getUserByMobile(String mobile) {
        return this.userMapper.selectByMobile(mobile);
    }

    @Override
    public PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO) {
        return this.userMapper.selectPage(reqVO, this.getDeptCondition(reqVO.getDeptId()));
    }

    @Override
    public AdminUserDO getUser(Long id) {
        return this.userMapper.selectById(id);
    }

    @Override
    public List<AdminUserDO> getUsersByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        return this.userMapper.selectListByDeptIds(deptIds);
    }

    @Override
    public List<AdminUserDO> getUsersByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = convertSet(this.userPostMapper.selectListByPostIds(postIds), UserPostDO::getUserId);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return this.userMapper.selectBatchIds(userIds);
    }

    @Override
    public List<AdminUserDO> getUsers(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return this.userMapper.selectBatchIds(ids);
    }

    @Override
    public void validUsers(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // ??????????????????
        List<AdminUserDO> users = this.userMapper.selectBatchIds(ids);
        Map<Long, AdminUserDO> userMap = CollectionUtils.convertMap(users, AdminUserDO::getId);
        // ??????
        ids.forEach(id -> {
            AdminUserDO user = userMap.get(id);
            if (user == null) {
                throw exception(USER_NOT_EXISTS);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
                throw exception(USER_IS_DISABLE, user.getNickname());
            }
        });
    }

    @Override
    public List<AdminUserDO> getUsers(UserExportReqVO reqVO) {
        return this.userMapper.selectList(reqVO, this.getDeptCondition(reqVO.getDeptId()));
    }

    @Override
    public List<AdminUserDO> getUsersByNickname(String nickname) {
        return this.userMapper.selectListByNickname(nickname);
    }

    @Override
    public List<AdminUserDO> getUsersByUsername(String username) {
        return this.userMapper.selectListByUsername(username);
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param deptId ????????????
     * @return ??????????????????
     */
    @Override
    public Set<Long> getDeptCondition(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = convertSet(this.deptService.getDeptsByParentIdFromCache(
            deptId, true), DeptDO::getId);
        deptIds.add(deptId); // ????????????
        return deptIds;
    }

    @Override
    public void updateUserByTeacherPhone(String oldPhone, String newPhone) {
        TeacherRespDTO teacherRespDTO = teacherApi.getTeacherByPhone(newPhone);
        if (teacherRespDTO != null) {
            AdminUserDO adminUserDO = getUserByUsername(oldPhone);
            if (adminUserDO != null) {
                // ??????????????????????????????????????????????????????????????????
                adminUserDO.setUsername(newPhone); // ????????????????????????????????????
                adminUserDO.setNickname(teacherRespDTO.getName());
                adminUserDO.setSex(Integer.valueOf(teacherRespDTO.getSex()));
                this.userMapper.updateById(adminUserDO);
            }
        }
    }

    @Override
    public void updateUserByStudentUid(String oldUid, String newUid) {
        StudentRespDTO studentRespDTO = studentApi.getStudentByUid(newUid);
        if (studentRespDTO != null) {
            AdminUserDO adminUserDO = getUserByUsername(oldUid); // ????????????????????????????????????
            if (adminUserDO != null) {
                // ?????????????????????????????????????????????
                adminUserDO.setUsername(newUid);
                adminUserDO.setNickname(studentRespDTO.getStudentName());
                adminUserDO.setDeptId(studentRespDTO.getDeptId());
                adminUserDO.setSex(Integer.valueOf(studentRespDTO.getSex()));
                this.userMapper.updateById(adminUserDO);
            }
        }
    }


    private void checkCreateOrUpdate(Long id, String username, String mobile, String email,
                                     Long deptId, Set<Long> postIds) {
        // ??????????????????
        this.checkUserExists(id);
        // ?????????????????????
        this.checkUsernameUnique(id, username);
        // ?????????????????????
        this.checkMobileUnique(id, mobile);
        // ??????????????????
        this.checkEmailUnique(id, email);
        // ??????????????????????????????
        this.deptService.validDepts(CollectionUtils.singleton(deptId));
        // ??????????????????????????????
        this.postService.validPosts(postIds);
    }

    @VisibleForTesting
    public void checkUserExists(Long id) {
        if (id == null) {
            return;
        }
        AdminUserDO user = this.userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    @VisibleForTesting
    public void checkUsernameUnique(Long id, String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AdminUserDO user = this.userMapper.selectByUsername(username);
        if (user == null) {
            return;
        }
        // ?????? id ?????????????????????????????????????????? id ?????????
        if (id == null) {
            throw exception(USER_USERNAME_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_USERNAME_EXISTS);
        }
    }

    @VisibleForTesting
    public void checkEmailUnique(Long id, String email) {
        if (StrUtil.isBlank(email)) {
            return;
        }
        AdminUserDO user = this.userMapper.selectByEmail(email);
        if (user == null) {
            return;
        }
        // ?????? id ?????????????????????????????????????????? id ?????????
        if (id == null) {
            throw exception(USER_EMAIL_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_EMAIL_EXISTS);
        }
    }

    @VisibleForTesting
    public void checkMobileUnique(Long id, String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return;
        }
        AdminUserDO user = this.userMapper.selectByMobile(mobile);
        if (user == null) {
            return;
        }
        // ?????? id ?????????????????????????????????????????? id ?????????
        if (id == null) {
            throw exception(USER_MOBILE_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_MOBILE_EXISTS);
        }
    }

    /**
     * ???????????????
     *
     * @param id          ?????? id
     * @param oldPassword ?????????
     */
    @VisibleForTesting
    public void checkOldPassword(Long id, String oldPassword) {
        AdminUserDO user = this.userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (!this.isPasswordMatch(oldPassword, user.getPassword())) {
            throw exception(USER_PASSWORD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // ??????????????????????????????????????????
    public UserImportRespVO importUsers(List<UserImportExcelVO> importUsers, boolean isUpdateSupport) {
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        UserImportRespVO respVO = UserImportRespVO.builder().createUsernames(new ArrayList<>())
            .updateUsernames(new ArrayList<>()).failureUsernames(new LinkedHashMap<>()).build();
        importUsers.forEach(importUser -> {
            // ??????????????????????????????????????????
            try {
                this.checkCreateOrUpdate(null, null, importUser.getMobile(), importUser.getEmail(),
                    importUser.getDeptId(), null);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(importUser.getUsername(), ex.getMessage());
                return;
            }
            // ???????????????????????????????????????
            AdminUserDO existUser = this.userMapper.selectByUsername(importUser.getUsername());
            if (existUser == null) {
                this.userMapper.insert(UserConvert.INSTANCE.convert(importUser)
                    .setPassword(this.encodePassword(this.userInitPassword))); // ??????????????????
                respVO.getCreateUsernames().add(importUser.getUsername());
                return;
            }
            // ???????????????????????????????????????
            if (!isUpdateSupport) {
                respVO.getFailureUsernames().put(importUser.getUsername(), USER_USERNAME_EXISTS.getMsg());
                return;
            }
            AdminUserDO updateUser = UserConvert.INSTANCE.convert(importUser);
            updateUser.setId(existUser.getId());
            this.userMapper.updateById(updateUser);
            respVO.getUpdateUsernames().add(importUser.getUsername());
        });
        return respVO;
    }

    @Override
    public List<AdminUserDO> getUsersByStatus(Integer status) {
        return this.userMapper.selectListByStatus(status);
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        return this.passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * ?????????????????????
     *
     * @param password ??????
     * @return ??????????????????
     */
    private String encodePassword(String password) {
        return this.passwordEncoder.encode(password);
    }

}
