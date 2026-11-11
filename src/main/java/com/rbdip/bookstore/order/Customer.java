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

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String address;
    private String phone;

    protected Customer() {
    }

    String getFullName() {
        return lastName.isEmpty() ? firstName : firstName + " " + lastName;
    }

    String getAddress() {
        return address;
    }

    String getPhone() {
        return phone;
    }
}
