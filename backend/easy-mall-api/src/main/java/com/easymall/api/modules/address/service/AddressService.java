package com.easymall.api.modules.address.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.Address;
import com.easymall.api.mapper.AddressMapper;
import com.easymall.api.modules.address.dto.AddressForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务。
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;

    public List<Address> list() {
        return addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, UserContext.userId())
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId));
    }

    public Address detail(Long id) {
        return requireOwnAddress(id);
    }

    @Transactional
    public Address create(AddressForm form) {
        Long userId = UserContext.userId();
        Address address = new Address();
        address.setUserId(userId);
        applyForm(address, form);
        boolean first = addressMapper.selectCount(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId)) == 0;
        if (first || Integer.valueOf(1).equals(form.isDefault())) {
            clearDefault(userId);
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        addressMapper.insert(address);
        return address;
    }

    @Transactional
    public Address update(Long id, AddressForm form) {
        Address address = requireOwnAddress(id);
        applyForm(address, form);
        if (Integer.valueOf(1).equals(form.isDefault())) {
            clearDefault(address.getUserId());
            address.setIsDefault(1);
        }
        if (addressMapper.updateById(address) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT);
        }
        return address;
    }

    public void delete(Long id) {
        Address address = requireOwnAddress(id);
        addressMapper.deleteById(address.getId());
    }

    @Transactional
    public void setDefault(Long id) {
        Address address = requireOwnAddress(id);
        clearDefault(address.getUserId());
        address.setIsDefault(1);
        if (addressMapper.updateById(address) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT);
        }
    }

    Address requireOwnAddress(Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, Wrappers.<Address>lambdaUpdate()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .set(Address::getIsDefault, 0));
    }

    private void applyForm(Address address, AddressForm form) {
        address.setReceiver(form.receiver());
        address.setPhone(form.phone());
        address.setProvince(form.province());
        address.setCity(form.city());
        address.setDistrict(form.district());
        address.setDetail(form.detail());
    }
}
