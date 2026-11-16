package com.fzu.addressbook.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 单条联系方式。 */
public class MethodRequest {

    @NotBlank(message = "联系方式类型不能为空")
    @Pattern(regexp = "phone|email|social|address|other",
            message = "联系方式类型只能是 phone / email / social / address / other")
    private String type;

    @NotBlank(message = "联系方式内容不能为空")
    @Size(max = 120, message = "联系方式内容最多 120 个字符")
    private String value;

    public MethodRequest() {
    }

    public MethodRequest(String type, String value) {
        this.type = type;
        this.value = value;
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
