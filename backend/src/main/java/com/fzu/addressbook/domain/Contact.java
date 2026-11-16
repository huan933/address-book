package com.fzu.addressbook.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人，对应 contact 表。
 *
 * <p>字段与前端 localStorage 里的数据结构保持一致（{@code favorite / createdAt / updatedAt}），
 * 联系方式为一对多关系（{@code contact_method}）。</p>
 */
@Entity
@Table(name = "contact")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 40, message = "姓名最多 40 个字符")
    @Column(nullable = false, length = 40)
    private String name;

    @Size(max = 60, message = "单位最多 60 个字符")
    @Column(length = 60)
    private String org;

    @Size(max = 80, message = "备注最多 80 个字符")
    @Column(length = 80)
    private String note;

    @Size(max = 300, message = "说明最多 300 个字符")
    @Column(length = 300)
    private String memo;

    @Column(nullable = false)
    private boolean favorite;

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private long updatedAt;

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<ContactMethod> methods = new ArrayList<>();

    protected Contact() {
    }

    public Contact(String name) {
        this.name = name;
    }

    /** 替换全部联系方式，用于 PUT 更新（配合 orphanRemoval 会自动删旧行）。 */
    public void replaceMethods(List<ContactMethod> newMethods) {
        methods.clear();
        newMethods.forEach(this::addMethod);
    }

    public void addMethod(ContactMethod method) {
        method.setContact(this);
        methods.add(method);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ContactMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<ContactMethod> methods) {
        this.methods = methods;
    }
}
