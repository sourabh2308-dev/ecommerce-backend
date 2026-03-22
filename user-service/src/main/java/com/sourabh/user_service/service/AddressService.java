package com.sourabh.user_service.service;

import com.sourabh.user_service.dto.request.AddressRequest;
import com.sourabh.user_service.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getAddresses(String userUuid);

    AddressResponse addAddress(String userUuid, AddressRequest request);

    AddressResponse updateAddress(String userUuid, String addressUuid, AddressRequest request);

    void deleteAddress(String userUuid, String addressUuid);

    AddressResponse setDefaultAddress(String userUuid, String addressUuid);
}
