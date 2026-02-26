package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.dto.request.AddressRequest;
import com.sourabh.user_service.dto.response.AddressResponse;
import com.sourabh.user_service.entity.Address;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.exception.UserNotFoundException;
import com.sourabh.user_service.exception.UserStateException;
import com.sourabh.user_service.repository.AddressRepository;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    private static final int MAX_ADDRESSES = 10;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String userUuid) {
        User user = findUser(userUuid);
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(String userUuid, AddressRequest request) {
        User user = findUser(userUuid);

        if (addressRepository.countByUser(user) >= MAX_ADDRESSES) {
            throw new UserStateException("Maximum " + MAX_ADDRESSES + " addresses allowed");
        }

        // If this is the first address or marked as default, handle default flag
        boolean makeDefault = request.isDefault() || addressRepository.countByUser(user) == 0;
        if (makeDefault) {
            clearDefaultAddress(user);
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .isDefault(makeDefault)
                .build();

        addressRepository.save(address);
        log.info("Address added: userUuid={}, addressUuid={}", userUuid, address.getUuid());
        return mapToResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String userUuid, String addressUuid, AddressRequest request) {
        User user = findUser(userUuid);
        Address address = addressRepository.findByUuidAndUser(addressUuid, user)
                .orElseThrow(() -> new UserStateException("Address not found"));

        address.setLabel(request.getLabel());
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());

        if (request.isDefault() && !address.isDefault()) {
            clearDefaultAddress(user);
            address.setDefault(true);
        }

        addressRepository.save(address);
        log.info("Address updated: addressUuid={}", addressUuid);
        return mapToResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(String userUuid, String addressUuid) {
        User user = findUser(userUuid);
        Address address = addressRepository.findByUuidAndUser(addressUuid, user)
                .orElseThrow(() -> new UserStateException("Address not found"));
        addressRepository.delete(address);
        log.info("Address deleted: addressUuid={}", addressUuid);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(String userUuid, String addressUuid) {
        User user = findUser(userUuid);
        Address address = addressRepository.findByUuidAndUser(addressUuid, user)
                .orElseThrow(() -> new UserStateException("Address not found"));

        clearDefaultAddress(user);
        address.setDefault(true);
        addressRepository.save(address);

        log.info("Default address set: addressUuid={}", addressUuid);
        return mapToResponse(address);
    }

    private void clearDefaultAddress(User user) {
        addressRepository.findByUserAndIsDefaultTrue(user).ifPresent(existing -> {
            existing.setDefault(false);
            addressRepository.save(existing);
        });
    }

    private User findUser(String userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .uuid(address.getUuid())
                .label(address.getLabel())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
