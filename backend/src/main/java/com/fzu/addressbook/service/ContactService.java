package com.fzu.addressbook.service;

import com.fzu.addressbook.domain.Contact;
import com.fzu.addressbook.domain.ContactMethod;
import com.fzu.addressbook.repository.ContactRepository;
import com.fzu.addressbook.web.dto.ContactRequest;
import com.fzu.addressbook.web.dto.MethodRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 联系人业务逻辑：增删改查、收藏、以及 Excel 导入导出。
 *
 * <p>导入导出与前端保持一致：一名联系人一条记录、每类信息一列，
 * 超出列宽的联系方式会被合并进「地址」列，避免信息丢失。</p>
 */
@Service
public class ContactService {

    /** 允许多个取值合并的列宽上限，与前端 HEADERS 一一对应。 */
    private static final int PHONE_COLS = 3;
    private static final int EMAIL_COLS = 2;
    private static final int SOCIAL_COLS = 2;

    private static final Set<String> TYPES = Set.of("phone", "email", "social", "address", "other");

    private final ContactRepository repository;

    public ContactService(ContactRepository repository) {
        this.repository = repository;
    }

    public List<Contact> list(boolean favoriteOnly, String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Contact> source = favoriteOnly
                ? repository.findByFavoriteTrueOrderByNameAsc()
                : repository.findAllByOrderByNameAsc();
        if (query.isEmpty()) {
            return source;
        }
        return source.stream().filter(c -> matches(c, query)).toList();
    }

    private boolean matches(Contact c, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append(nvl(c.getName())).append(' ')
                .append(nvl(c.getOrg())).append(' ')
                .append(nvl(c.getNote())).append(' ')
                .append(nvl(c.getMemo()));
        for (ContactMethod m : c.getMethods()) {
            sb.append(' ').append(nvl(m.getValue()));
        }
        return sb.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    public Contact get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("联系人不存在：" + id));
    }

    @Transactional
    public Contact create(ContactRequest request) {
        List<ContactMethod> methods = buildMethods(request.getMethods());
        Contact contact = new Contact(request.getName().trim());
        applyFields(contact, request);
        contact.replaceMethods(methods);
        long now = System.currentTimeMillis();
        contact.setCreatedAt(now);
        contact.setUpdatedAt(now);
        return repository.save(contact);
    }

    @Transactional
    public Contact update(Long id, ContactRequest request) {
        Contact contact = get(id);
        applyFields(contact, request);
        contact.replaceMethods(buildMethods(request.getMethods()));
        contact.setUpdatedAt(System.currentTimeMillis());
        return repository.save(contact);
    }

    @Transactional
    public Contact toggleFavorite(Long id) {
        Contact contact = get(id);
        contact.setFavorite(!contact.isFavorite());
        contact.setUpdatedAt(System.currentTimeMillis());
        return repository.save(contact);
    }

    @Transactional
    public void delete(Long id) {
        Contact contact = get(id);
        repository.delete(contact);
    }

    private void applyFields(Contact contact, ContactRequest request) {
        contact.setName(request.getName().trim());
        contact.setOrg(trimToNull(request.getOrg()));
        contact.setNote(trimToNull(request.getNote()));
        contact.setMemo(trimToNull(request.getMemo()));
        contact.setFavorite(request.isFavorite());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 校验并转换请求里的联系方式：至少一条，类型必须在允许集合内。 */
    private List<ContactMethod> buildMethods(List<MethodRequest> requests) {
        List<ContactMethod> methods = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("请至少填写一条联系方式");
        }
        for (MethodRequest r : requests) {
            String type = r.getType() == null ? "" : r.getType().trim();
            String value = r.getValue() == null ? "" : r.getValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            if (!TYPES.contains(type)) {
                throw new IllegalArgumentException("不支持的联系方式类型：" + type);
            }
            methods.add(new ContactMethod(type, value));
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("请至少填写一条联系方式");
        }
        return methods;
    }

    /* ---------------- 导出 ---------------- */

    /** 按姓名排序后压平成 12 列的行数据。 */
    public List<List<String>> exportRows() {
        Collator collator = Collator.getInstance(Locale.CHINA);
        List<Contact> contacts = new ArrayList<>(repository.findAllByOrderByNameAsc());
        contacts.sort((a, b) -> collator.compare(nvl(a.getName()), nvl(b.getName())));

        List<List<String>> rows = new ArrayList<>();
        for (Contact c : contacts) {
            List<String> phone = new ArrayList<>();
            List<String> email = new ArrayList<>();
            List<String> social = new ArrayList<>();
            List<String> address = new ArrayList<>();
            for (ContactMethod m : c.getMethods()) {
                switch (m.getType()) {
                    case "phone" -> phone.add(m.getValue());
                    case "email" -> email.add(m.getValue());
                    case "social" -> social.add(m.getValue());
                    default -> address.add(m.getValue());
                }
            }
            List<String> row = new ArrayList<>();
            row.add(nvl(c.getName()));
            row.add(nvl(c.getOrg()));
            row.add(c.isFavorite() ? "是" : "否");
            row.add(nvl(c.getNote()));
            row.add(cell(phone, 0));
            row.add(cell(phone, 1));
            row.add(cell(phone, 2));
            row.add(cell(email, 0));
            row.add(cell(email, 1));
            row.add(cell(social, 0));
            row.add(cell(social, 1));
            row.add(String.join("；", address));
            rows.add(row);
        }
        return rows;
    }

    private static String cell(List<String> values, int index) {
        return index < values.size() ? values.get(index) : "";
    }

    /* ---------------- 导入 ---------------- */

    /** 导入结果统计。 */
    public record ImportResult(int added, int duplicates, int invalid) {
    }

    /**
     * 按表头识别列并导入。前 5 行内自动寻找表头行，找不到「姓名」列直接报错；
     * 缺姓名或没有联系方式的行计入 invalid，重名计入 duplicates。
     */
    @Transactional
    public ImportResult importRows(List<List<String>> table) {
        if (table == null || table.isEmpty()) {
            throw new IllegalArgumentException("文件内容为空");
        }
        HeaderMap header = locateHeader(table);
        if (header == null) {
            throw new IllegalArgumentException("没有找到「姓名」列，请确认表头");
        }

        int added = 0;
        int duplicates = 0;
        int invalid = 0;
        long now = System.currentTimeMillis();

        for (int r = header.rowIndex() + 1; r < table.size(); r++) {
            List<String> row = table.get(r);
            String name = header.value(row, "name");
            if (name.isEmpty()) {
                invalid++;
                continue;
            }
            List<ContactMethod> methods = new ArrayList<>();
            for (int c = 0; c < row.size(); c++) {
                String key = header.byColumn.get(c);
                String value = row.get(c) == null ? "" : row.get(c).trim();
                if (key == null || value.isEmpty() || !TYPES.contains(key)) {
                    continue;
                }
                methods.add(new ContactMethod(key, value));
            }
            if (methods.isEmpty()) {
                invalid++;
                continue;
            }
            if (repository.findFirstByName(name).isPresent()) {
                duplicates++;
                continue;
            }
            Contact contact = new Contact(name);
            contact.setOrg(emptyToNull(header.value(row, "org")));
            contact.setNote(emptyToNull(header.value(row, "note")));
            contact.setMemo(emptyToNull(header.value(row, "memo")));
            String fav = header.value(row, "fav");
            contact.setFavorite(fav.matches("是|y|yes|true|1"));
            contact.replaceMethods(methods);
            contact.setCreatedAt(now);
            contact.setUpdatedAt(now);
            repository.save(contact);
            added++;
        }
        return new ImportResult(added, duplicates, invalid);
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    /** 表头映射：列号 → 逻辑字段。 */
    private record HeaderMap(int rowIndex, java.util.Map<Integer, String> byColumn) {

        String value(List<String> row, String key) {
            for (java.util.Map.Entry<Integer, String> e : byColumn.entrySet()) {
                if (e.getValue().equals(key) && e.getKey() < row.size()) {
                    String v = row.get(e.getKey());
                    return v == null ? "" : v.trim();
                }
            }
            return "";
        }
    }

    private HeaderMap locateHeader(List<List<String>> table) {
        for (int r = 0; r < Math.min(5, table.size()); r++) {
            java.util.Map<Integer, String> map = new java.util.LinkedHashMap<>();
            List<String> row = table.get(r);
            for (int c = 0; c < row.size(); c++) {
                String key = classify(row.get(c));
                if (key != null) {
                    map.putIfAbsent(c, key);
                }
            }
            if (map.containsValue("name")) {
                return new HeaderMap(r, map);
            }
        }
        return null;
    }

    /** 把中/英文字段名归类到规范键，容忍「手机号码」「e-mail」等写法。 */
    private static String classify(String raw) {
        if (raw == null) {
            return null;
        }
        String h = raw.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
        if (h.isEmpty()) {
            return null;
        }
        if (h.matches(".*(姓名|名字|name).*")) {
            return "name";
        }
        if (h.matches(".*(单位|公司|部门|org|company).*")) {
            return "org";
        }
        if (h.matches(".*(收藏|favor).*")) {
            return "fav";
        }
        if (h.matches(".*(备注).*")) {
            return "note";
        }
        if (h.matches(".*(说明|memo).*")) {
            return "memo";
        }
        if (h.matches(".*(电话|手机|tel|phone).*")) {
            return "phone";
        }
        if (h.matches(".*(邮箱|邮件|email|mail).*")) {
            return "email";
        }
        if (h.matches(".*(社交|微信|wechat|social).*")) {
            return "social";
        }
        if (h.matches(".*(地址|address).*")) {
            return "address";
        }
        return null;
    }
}
