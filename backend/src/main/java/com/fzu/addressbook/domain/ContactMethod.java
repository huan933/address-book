package com.fzu.addressbook.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 一条联系方式，对应 contact_method 表。
 *
 * <p>一个联系人可以挂任意多条联系方式，type 取值与前端保持一致：
 * {@code phone / email / social / address / other}。</p>
 */
@Entity
@Table(name = "contact_method")
public class ContactMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属联系人；序列化时忽略，避免 JSON 循环引用。 */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @NotBlank(message = "联系方式类型不能为空")
    @Column(nullable = false, length = 16)
    private String type;

    @NotBlank(message = "联系方式内容不能为空")
    @Size(max = 120, message = "联系方式内容最多 120 个字符")
    // H2 2.x 里 VALUE 是保留字，列名改用 contact_value；对外 JSON 字段仍是 value
    @Column(name = "contact_value", nullable = false, length = 120)
    private String value;

    protected ContactMethod() {
    }

    public ContactMethod(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
