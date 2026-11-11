package com.rbdip.bookstore.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
class Customer {

    @Id
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String address;
    private String phone;

    protected Customer() {
    }

    String getFullName() {
        return fullName;
    }

    String getAddress() {
        return address;
    }

    String getPhone() {
        return phone;
    }
}
