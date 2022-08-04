package cn.iocoder.yudao.module.stucms.controller.admin.student;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.operatelog.core.annotations.OperateLog;
import cn.iocoder.yudao.module.stucms.controller.admin.student.vo.*;
import cn.iocoder.yudao.module.stucms.convert.student.StudentConvert;
import cn.iocoder.yudao.module.stucms.dal.dataobject.student.StudentDO;
import cn.iocoder.yudao.module.stucms.service.student.StudentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.operatelog.core.enums.OperateTypeEnum.EXPORT;

@Api(tags = "管理后台 - 学生管理")
@RestController
@RequestMapping("/stucms/student")
@Validated
public class StudentController {

    @Resource
    private StudentService studentService;

    @PostMapping("/create")
    @ApiOperation("创建学生管理")
    @PreAuthorize("@ss.hasPermission('stucms:student:create')")
    public CommonResult<Long> createStudent(@Valid @RequestBody StudentCreateReqVO createReqVO) {
        return success(this.studentService.createStudent(createReqVO));
    }

    @PutMapping("/update")
    @ApiOperation("更新学生管理")
    @PreAuthorize("@ss.hasPermission('stucms:student:update')")
    public CommonResult<Boolean> updateStudent(@Valid @RequestBody StudentUpdateReqVO updateReqVO) {
        this.studentService.updateStudent(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @ApiOperation("删除学生管理")
    @ApiImplicitParam(name = "id", value = "编号", required = true, dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermission('stucms:student:delete')")
    public CommonResult<Boolean> deleteStudent(@RequestParam("id") Long id) {
        this.studentService.deleteStudent(id);
        return success(true);
    }

    @GetMapping("/get")
    @ApiOperation("获得学生管理")
    @ApiImplicitParam(name = "id", value = "编号", required = true, example = "1024", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermission('stucms:student:query')")
    public CommonResult<StudentRespVO> getStudent(@RequestParam("id") Long id) {
        StudentDO student = this.studentService.getStudent(id);
        return success(StudentConvert.INSTANCE.convert(student));
    }

    @GetMapping("/simple-page")
    @ApiOperation("获得简单学生分页列表")
    @PreAuthorize("@ss.hasPermission('stucms:student:query')")
    public CommonResult<PageResult<StudentSimpleRespVO>> getStudentSimplePage(@Valid StudentSimplePageReqVO pageVO) {
        PageResult<StudentDO> list = this.studentService.getStudentSimplePage(pageVO);
        return success(StudentConvert.INSTANCE.convertSimplePage(list));
    }

    @GetMapping("/page")
    @ApiOperation("获得学生管理分页")
    @PreAuthorize("@ss.hasPermission('stucms:student:query')")
    public CommonResult<PageResult<StudentRespVO>> getStudentPage(@Valid StudentPageReqVO pageVO) {
        PageResult<StudentDO> pageResult = this.studentService.getStudentPage(pageVO);
        return success(StudentConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/export-excel")
    @ApiOperation("导出学生管理 Excel")
    @PreAuthorize("@ss.hasPermission('stucms:student:export')")
    @OperateLog(type = EXPORT)
    public void exportStudentExcel(@Valid StudentExportReqVO exportReqVO,
                                   HttpServletResponse response) throws IOException {
        List<StudentDO> list = this.studentService.getStudentList(exportReqVO);
        // 导出 Excel
        List<StudentExcelVO> datas = StudentConvert.INSTANCE.convertList02(list);
        ExcelUtils.write(response, "学生管理.xls", "数据", StudentExcelVO.class, datas);
    }

}
