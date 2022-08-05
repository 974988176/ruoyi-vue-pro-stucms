package cn.iocoder.yudao.module.stucms.controller.admin.score.vo.personal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 图3的序列数据
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Chart3SeriesRespVO {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("图表类型")
    private String type = "line";

    @ApiModelProperty("线条平滑")
    private Boolean smooth = true;

    @ApiModelProperty("每个折线图点的数据")
    private List<Chart3DataVo> data;
}
