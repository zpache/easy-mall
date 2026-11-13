package cn.easykits.admin.modules.setting.vo;

import cn.easykits.admin.entity.DeliveryCompany;

import java.time.LocalDateTime;

/**
 * 快递公司列表项。
 */
public record DeliveryCompanyVO(
        Long id,
        String name,
        String code,
        boolean phoneRequired,
        int sort,
        int status,
        String remark,
        LocalDateTime updatedAt
) {

    public static DeliveryCompanyVO of(DeliveryCompany company) {
        return new DeliveryCompanyVO(
                company.getId(),
                company.getName(),
                company.getCode(),
                Integer.valueOf(1).equals(company.getPhoneRequired()),
                company.getSort() == null ? 0 : company.getSort(),
                company.getStatus() == null ? 0 : company.getStatus(),
                company.getRemark(),
                company.getUpdatedAt()
        );
    }
}
