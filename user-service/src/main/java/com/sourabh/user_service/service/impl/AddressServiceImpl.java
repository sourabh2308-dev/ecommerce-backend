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

/**
 * Implementation of {@link AddressService} for managing user shipping/billing addresses.
 *
 * <p>Addresses are stored in the {@code addresses} table with a many-to-one relationship
 * to {@link User}. A maximum of {@value #MAX_ADDRESSES} addresses are allowed per user.
 * Exactly one address may be marked as the default at any time; setting a new default
 * automatically clears the previous one.</p>
 *
 * <p>All write operations are wrapped in Spring-managed transactions to guarantee
 * atomicity (e.g. clearing the old default and setting the new one happen together).</p>
 *
 * @see AddressService
 * @see AddressRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    /** Repository for {@link Address} persistence operations. */
    private final AddressRepository addressRepository;

    /** Repository for {@link User} lookups. */
    private final UserRepository userRepository;

    /** Maximum number of addresses a single user may store. */
    private static final int MAX_ADDRESSES = 10;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String userUuid) {
        User user = findUser(userUuid);
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AddressResponse addAddress(String userUuid, AddressRequest request) {
        User user = findUser(userUuid);

        if (addressRepository.countByUser(user) >= MAX_ADDRESSES) {
            throw new UserStateException("Maximum " + MAX_ADDRESSES + " addresses allowed");
        }

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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteAddress(String userUuid, String addressUuid) {
        User user = findUser(userUuid);
        Address address = addressRepository.findByUuidAndUser(addressUuid, user)
                .orElseThrow(() -> new UserStateException("Address not found"));
        addressRepository.delete(address);
        log.info("Address deleted: addressUuid={}", addressUuid);
    }

    /** {@inheritDoc} */
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

    /**
     * Clears the {@code isDefault} flag on the user's current default address, if any.
     *
     * @param user the user whose default address should be cleared
     */
    private void clearDefaultAddress(User user) {
        addressRepository.findByUserAndIsDefaultTrue(user).ifPresent(existing -> {
            existing.setDefault(false);
            addressRepository.save(existing);
        });
    }

    /**
     * Looks up a {@link User} by UUID or throws {@link UserNotFoundException}.
     *
     * @param userUuid the UUID to search for
     * @return the matching {@link User} entity
     */
    private User findUser(String userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Maps an {@link Address} entity to an {@link AddressResponse} DTO.
     *
     * @param address the entity to convert
     * @return the corresponding response DTO
     */
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
