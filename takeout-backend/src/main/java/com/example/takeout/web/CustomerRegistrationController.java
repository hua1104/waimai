package com.example.takeout.web;

import com.example.takeout.entity.Customer;
import com.example.takeout.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/customers")
public class CustomerRegistrationController {

    private final CustomerRepository customerRepository;

    public CustomerRegistrationController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody RegisterPayload payload) {
        String err = validate(payload);
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }
        String username = payload.getUsername().trim();
        if (customerRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "用户名已存在"));
        }

        Customer c = new Customer();
        c.setUsername(username);
        c.setPassword(payload.getPassword());
        c.setRealName(payload.getRealName().trim());
        c.setPhone(payload.getPhone().trim());
        c.setStatus("ACTIVE");

        Customer saved = customerRepository.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "status", "OK"));
    }

    private static String validate(RegisterPayload payload) {
        if (payload == null) return "参数不完整";
        if (isBlank(payload.getUsername())) return "用户名不能为空";
        if (isBlank(payload.getPassword())) return "密码不能为空";
        if (payload.getPassword().length() < 6) return "密码至少 6 位";
        if (isBlank(payload.getRealName())) return "真实姓名不能为空";
        if (isBlank(payload.getPhone())) return "手机号不能为空";
        if (payload.getUsername().trim().length() > 50) return "用户名过长";
        if (payload.getRealName().trim().length() > 50) return "姓名过长";
        if (payload.getPhone().trim().length() > 20) return "手机号过长";
        return null;
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static class RegisterPayload {
        private String username;
        private String password;
        private String realName;
        private String phone;

        public RegisterPayload() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}

