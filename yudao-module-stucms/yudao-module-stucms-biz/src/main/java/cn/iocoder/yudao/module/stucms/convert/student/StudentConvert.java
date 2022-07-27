package cn.iocoder.yudao.module.stucms.convert.student;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.stucms.controller.admin.student.vo.StudentCreateReqVO;
import cn.iocoder.yudao.module.stucms.controller.admin.student.vo.StudentExcelVO;
import cn.iocoder.yudao.module.stucms.controller.admin.student.vo.StudentRespVO;
import cn.iocoder.yudao.module.stucms.controller.admin.student.vo.StudentUpdateReqVO;
import cn.iocoder.yudao.module.stucms.dal.dataobject.student.StudentDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 学生管理 Convert
 *
 * @author 华
 */
@Mapper
public interface StudentConvert {

    StudentConvert INSTANCE = Mappers.getMapper(StudentConvert.class);

    StudentDO convert(StudentCreateReqVO bean);

    StudentDO convert(StudentUpdateReqVO bean);

    StudentRespVO convert(StudentDO bean);

    List<StudentRespVO> convertList(List<StudentDO> list);

    PageResult<StudentRespVO> convertPage(PageResult<StudentDO> page);

    List<StudentExcelVO> convertList02(List<StudentDO> list);

}
