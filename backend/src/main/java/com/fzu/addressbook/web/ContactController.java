package com.fzu.addressbook.web;

import com.fzu.addressbook.domain.Contact;
import com.fzu.addressbook.service.ContactService;
import com.fzu.addressbook.web.dto.ContactRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 联系人的 REST 接口。 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    /** 列表；favorite=true 只看收藏，q 做姓名/单位/备注/联系方式的模糊搜索。 */
    @GetMapping
    public List<Contact> list(@RequestParam(required = false, defaultValue = "false") boolean favorite,
                              @RequestParam(required = false) String q) {
        return service.list(favorite, q);
    }

    @GetMapping("/{id}")
    public Contact detail(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Contact create(@Valid @RequestBody ContactRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Contact update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return service.update(id, request);
    }

    /** 收藏开关：切换一次取反，收藏数由前端自行统计。 */
    @PostMapping("/{id}/favorite")
    public Contact toggleFavorite(@PathVariable Long id) {
        return service.toggleFavorite(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
