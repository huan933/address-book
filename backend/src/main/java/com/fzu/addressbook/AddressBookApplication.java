package com.fzu.addressbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通讯录 Address Book 后端启动类。
 *
 * <p>默认端口 8080，数据库为内嵌 H2（文件模式，数据落在 ./data/addressbook.mv.db），
 * 无需安装任何外部数据库。</p>
 */
@SpringBootApplication
public class AddressBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddressBookApplication.class, args);
    }
}
