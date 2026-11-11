package com.rbdip.bookstore.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO customers (full_name, address, phone)
            VALUES (:fullName, :address, :phone)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertIfMissing(@Param("fullName") String fullName, @Param("address") String address,
            @Param("phone") String phone);

    Customer findByFullNameAndAddressAndPhone(String fullName, String address, String phone);
}
