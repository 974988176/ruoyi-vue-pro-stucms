package cn.iocoder.yudao.module.stucms.controller.admin.score;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.stucms.controller.admin.score.vo.personal.Chart1SeriesRespVO;
import cn.iocoder.yudao.module.stucms.service.score.ScorePersonalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;


@Validated
@Api(tags = {"管理后台 - 个人分析"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/stucms/score/personal")
public class ScorePersonalController {
    @Resource
    private ScorePersonalService scorePersonalService;

    @GetMapping("/chart1/{studentId}")
    @PreAuthorize("@ss.hasPermission('stucms:score:personal:query')")
    public CommonResult<List<Chart1SeriesRespVO>> getChart1(@ApiParam("学生ID")
                                                            @NotNull(message = "学生ID不能为空")
                                                            @PathVariable("studentId") Long studentId) {
        List<Chart1SeriesRespVO> seriesRespVOList = this.scorePersonalService.getChart1SeriesById(studentId);
        return success(seriesRespVOList);
    }
}
