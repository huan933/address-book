package com.fzu.addressbook.repository;

import com.fzu.addressbook.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 联系人数据访问层。 */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findAllByOrderByNameAsc();

    List<Contact> findByFavoriteTrueOrderByNameAsc();

    Optional<Contact> findFirstByName(String name);
}
