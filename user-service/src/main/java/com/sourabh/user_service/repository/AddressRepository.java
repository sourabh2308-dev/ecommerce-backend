package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.Address;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Address} entities.
 * <p>
 * Provides standard CRUD operations plus custom finder methods for
 * retrieving addresses by user, UUID, and default-flag status.
 * </p>
 *
 * @see Address
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Returns all addresses for the given user, ordered with the default
     * address first and then by most-recently created.
     *
     * @param user the owning {@link User}
     * @return ordered list of addresses
     */
    List<Address> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);

    /**
     * Finds an address by its public UUID scoped to a specific user.
     *
     * @param uuid the address UUID
     * @param user the owning {@link User}
     * @return the matching address, if any
     */
    Optional<Address> findByUuidAndUser(String uuid, User user);

    /**
     * Finds the user's current default address, if one has been set.
     *
     * @param user the owning {@link User}
     * @return the default address, if any
     */
    Optional<Address> findByUserAndIsDefaultTrue(User user);

    /**
     * Returns the total number of addresses stored for the given user.
     *
     * @param user the owning {@link User}
     * @return address count
     */
    int countByUser(User user);
}
