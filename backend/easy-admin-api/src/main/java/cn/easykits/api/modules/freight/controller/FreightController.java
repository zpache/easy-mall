package cn.easykits.api.modules.freight.controller;

import cn.easykits.api.common.api.Result;
import cn.easykits.api.common.api.ResultCode;
import cn.easykits.api.common.context.UserContext;
import cn.easykits.api.common.exception.BusinessException;
import cn.easykits.api.entity.Address;
import cn.easykits.api.mapper.AddressMapper;
import cn.easykits.api.modules.freight.service.FreightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 运费（小程序结算页预览）。下单时的实际运费以服务端下单计算为准。
 */
@Tag(name = "运费")
@RestController
@RequestMapping("/freight")
@RequiredArgsConstructor
public class FreightController {

    private final FreightService freightService;
    private final AddressMapper addressMapper;

    @Operation(summary = "运费预览（按收货地址与商品总额）")
    @GetMapping("/preview")
    public Result<FreightService.FreightResult> preview(@RequestParam Long addressId,
                                                        @RequestParam(required = false) BigDecimal totalAmount) {
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }
        return Result.ok(freightService.calculate(address, totalAmount));
    }
}
