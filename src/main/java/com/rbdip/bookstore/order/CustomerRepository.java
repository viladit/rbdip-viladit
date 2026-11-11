package com.rbdip.bookstore.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO customers (first_name, last_name, address, phone)
            SELECT first_name, last_name, :address, :phone FROM split_customer_name(:fullName)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertIfMissing(@Param("fullName") String fullName, @Param("address") String address,
            @Param("phone") String phone);

    @Query(value = """
            SELECT c.* FROM customers c
            JOIN split_customer_name(:fullName) n USING (first_name, last_name)
            WHERE c.address IS NOT DISTINCT FROM :address AND c.phone IS NOT DISTINCT FROM :phone
            """, nativeQuery = true)
    Customer findByContact(@Param("fullName") String fullName, @Param("address") String address,
            @Param("phone") String phone);
}
