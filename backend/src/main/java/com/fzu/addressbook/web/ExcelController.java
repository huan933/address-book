package com.fzu.addressbook.web;

import com.fzu.addressbook.service.ContactService;
import com.fzu.addressbook.service.ExcelSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Excel 导入导出与健康检查接口。 */
@RestController
@RequestMapping("/api/contacts")
public class ExcelController {

    private final ContactService service;

    public ExcelController(ContactService service) {
        this.service = service;
    }

    /** 导出全部联系人：一名联系人一行、每类信息一列。 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws IOException {
        byte[] bytes = ExcelSupport.toXlsx(service.exportRows());
        String filename = "通讯录_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /** 导入 xlsx / xls / csv，返回新增、重名跳过、无效行数。 */
    @PostMapping("/import")
    public Map<String, Integer> importFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的文件");
        }
        List<List<String>> table = ExcelSupport.read(file.getInputStream(), file.getOriginalFilename());
        ContactService.ImportResult result = service.importRows(table);
        return Map.of(
                "added", result.added(),
                "duplicates", result.duplicates(),
                "invalid", result.invalid());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
