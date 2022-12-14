package cn.iocoder.yudao.module.stucms.controller.admin.student.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@ApiModel(value = "管理后台 - 学生管理 Excel 导出 Request VO", description = "参数和 StudentPageReqVO 是一致的")
@Data
public class StudentExportReqVO {

    @ApiModelProperty(value = "学号", example = "3042013024")
    private String studentUid;

    @ApiModelProperty(value = "姓名", example = "张三")
    private String studentName;

    @ApiModelProperty(value = "性别", example = "男")
    private String sex;

    @ApiModelProperty(value = "手机", example = "18179871100")
    private String phone;

    @ApiModelProperty(value = "身份证号")
    private String sysid;

    @ApiModelProperty(value = "寄宿", example = "寄宿")
    private String jishu;

    @ApiModelProperty(value = "留守儿童", example = "是")
    private Integer liushou;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date[] createTime;

}
