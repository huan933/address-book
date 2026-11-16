package com.fzu.addressbook.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/** 新增 / 更新联系人的请求体，字段名与前端保持一致。 */
public class ContactRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 40, message = "姓名最多 40 个字符")
    private String name;

    @Size(max = 60, message = "单位最多 60 个字符")
    private String org;

    @Size(max = 80, message = "备注最多 80 个字符")
    private String note;

    @Size(max = 300, message = "说明最多 300 个字符")
    private String memo;

    private boolean favorite;

    @Valid
    private List<MethodRequest> methods = new ArrayList<>();

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

    public List<MethodRequest> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodRequest> methods) {
        this.methods = methods;
    }
}
