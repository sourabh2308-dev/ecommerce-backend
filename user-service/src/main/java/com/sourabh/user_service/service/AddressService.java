package com.sourabh.user_service.service;

import com.sourabh.user_service.dto.request.AddressRequest;
import com.sourabh.user_service.dto.response.AddressResponse;

import java.util.List;

/**
 * Service interface for managing user shipping/billing addresses.
 *
 * <p>Provides CRUD operations and default-address management.
 * Each user may store up to a configurable maximum number of addresses;
 * exactly one address may be flagged as the default at any time.</p>
 *
 * @see com.sourabh.user_service.service.impl.AddressServiceImpl
 */
public interface AddressService {

    /**
     * Retrieves all addresses belonging to the specified user,
     * ordered by default flag (default first) then by creation date descending.
     *
     * @param userUuid the UUID of the authenticated user
     * @return list of {@link AddressResponse} DTOs; empty list if none exist
     */
    List<AddressResponse> getAddresses(String userUuid);

    /**
     * Adds a new address for the given user.
     *
     * <p>If this is the user's first address or the request marks it as default,
     * any existing default address is cleared before persisting.</p>
     *
     * @param userUuid the UUID of the authenticated user
     * @param request  the address details to persist
     * @return the newly created {@link AddressResponse}
     * @throws com.sourabh.user_service.exception.UserStateException if the maximum address limit is reached
     */
    AddressResponse addAddress(String userUuid, AddressRequest request);

    /**
     * Updates an existing address identified by its UUID.
     *
     * @param userUuid    the UUID of the authenticated user (ownership check)
     * @param addressUuid the UUID of the address to update
     * @param request     the updated address fields
     * @return the updated {@link AddressResponse}
     * @throws com.sourabh.user_service.exception.UserStateException if the address is not found for the user
     */
    AddressResponse updateAddress(String userUuid, String addressUuid, AddressRequest request);

    /**
     * Permanently deletes an address belonging to the user.
     *
     * @param userUuid    the UUID of the authenticated user
     * @param addressUuid the UUID of the address to delete
     * @throws com.sourabh.user_service.exception.UserStateException if the address is not found for the user
     */
    void deleteAddress(String userUuid, String addressUuid);

    /**
     * Marks the specified address as the user's default, clearing any previously
     * set default address.
     *
     * @param userUuid    the UUID of the authenticated user
     * @param addressUuid the UUID of the address to set as default
     * @return the updated {@link AddressResponse} with {@code isDefault = true}
     * @throws com.sourabh.user_service.exception.UserStateException if the address is not found for the user
     */
    AddressResponse setDefaultAddress(String userUuid, String addressUuid);
}
