package com.fzu.addressbook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口层集成测试：用内存 H2，覆盖新增、收藏、搜索、删除以及 Excel 导入导出。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:addressbook-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AddressBookApiTest {

    @Autowired
    private MockMvc mvc;

    private static final String ONE_CONTACT = """
            {
              "name": "测试甲",
              "org": "福州大学",
              "note": "同组",
              "memo": "实验室 402",
              "favorite": false,
              "methods": [
                {"type": "phone", "value": "13800000001"},
                {"type": "email", "value": "test1@example.com"},
                {"type": "social", "value": "微信 tester_jia"}
              ]
            }
            """;

    @Test
    @DisplayName("新增联系人：三种联系方式都能落库并回显")
    void createContact() throws Exception {
        mvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ONE_CONTACT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("测试甲"))
                .andExpect(jsonPath("$.methods.length()").value(3));
    }

    @Test
    @DisplayName("没有任何联系方式时拒绝保存")
    void rejectContactWithoutMethods() throws Exception {
        mvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"无号码\",\"methods\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请至少填写一条联系方式"));
    }

    @Test
    @DisplayName("收藏开关：切换后收藏列表能查到，搜索命中单位")
    void toggleFavoriteAndSearch() throws Exception {
        mvc.perform(post("/api/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ONE_CONTACT)).andExpect(status().isCreated());

        String body = mvc.perform(get("/api/contacts").param("q", "测试甲"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = readId(body);

        mvc.perform(post("/api/contacts/" + id + "/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));

        mvc.perform(get("/api/contacts").param("favorite", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        mvc.perform(get("/api/contacts").param("q", "福州大学"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
    }

    @Test
    @DisplayName("导出返回合法 xlsx，且列顺序与前端 12 列契约一致")
    void exportReturnsXlsx() throws Exception {
        byte[] body = mvc.perform(get("/api/contacts/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(body.length).isGreaterThan(0);
        // xlsx 是 zip，前两个字节应为 PK
        assertThat(body[0]).isEqualTo((byte) 'P');
        assertThat(body[1]).isEqualTo((byte) 'K');
    }

    @Test
    @DisplayName("导入 CSV：新增成功，重名再导入被计入 duplicates")
    void importCsv() throws Exception {
        String csv = "姓名,单位,收藏,备注,电话1,邮箱1\n导入乙,福州大学,否,同组,13900000002,b@example.com\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "通讯录_测试.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/contacts/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(1))
                .andExpect(jsonPath("$.duplicates").value(0));

        mvc.perform(multipart("/api/contacts/import").file(new MockMultipartFile(
                        "file", "again.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(0))
                .andExpect(jsonPath("$.duplicates").value(1));
    }

    @Test
    @DisplayName("删除后详情返回 404")
    void deleteThenNotFound() throws Exception {
        mvc.perform(post("/api/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"删除丙\",\"methods\":[{\"type\":\"phone\",\"value\":\"13700000003\"}]}"))
                .andExpect(status().isCreated());

        String body = mvc.perform(get("/api/contacts").param("q", "删除丙"))
                .andReturn().getResponse().getContentAsString();
        long id = readId(body);

        mvc.perform(delete("/api/contacts/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/contacts/" + id)).andExpect(status().isNotFound());
    }

    private long readId(String json) {
        int idx = json.indexOf("\"id\":");
        assertThat(idx).isGreaterThanOrEqualTo(0);
        int start = idx + 5;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == ' ')) {
            end++;
        }
        return Long.parseLong(json.substring(start, end).trim());
    }
}
