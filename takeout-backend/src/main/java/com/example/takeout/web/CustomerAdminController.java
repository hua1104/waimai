package com.example.takeout.web;

import com.example.takeout.entity.Customer;
import com.example.takeout.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerAdminController {

    private final CustomerRepository customerRepository;

    public CustomerAdminController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<CustomerRow> list() {
        return customerRepository.findAll().stream()
                .map(c -> new CustomerRow(
                        c.getId(),
                        c.getUsername(),
                        c.getRealName(),
                        c.getPhone(),
                        c.getStatus(),
                        c.getCreatedAt() == null ? null : c.getCreatedAt().toString()
                ))
                .toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody CreatePayload payload) {
        String err = validateCreate(payload);
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
        c.setStatus(payload.getStatus() == null || payload.getStatus().isBlank() ? "ACTIVE" : payload.getStatus());

        Customer saved = customerRepository.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "status", "OK"));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody UpdatePayload payload) {
        Optional<Customer> opt = customerRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        String err = validateUpdate(payload);
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }

        Customer c = opt.get();
        if (payload.getUsername() != null && !payload.getUsername().trim().equalsIgnoreCase(c.getUsername())) {
            String nextUsername = payload.getUsername().trim();
            if (nextUsername.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "用户名不能为空"));
            }
            if (customerRepository.existsByUsername(nextUsername)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "用户名已存在"));
            }
            c.setUsername(nextUsername);
        }
        c.setRealName(payload.getRealName().trim());
        c.setPhone(payload.getPhone().trim());
        if (payload.getStatus() != null && !payload.getStatus().isBlank()) {
            c.setStatus(payload.getStatus());
        }
        customerRepository.save(c);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Customer> updateStatus(@PathVariable("id") Long id, @RequestBody StatusPayload payload) {
        Optional<Customer> customerOpt = customerRepository.findById(id);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Customer customer = customerOpt.get();
        customer.setStatus(payload.getStatus());
        Customer saved = customerRepository.save(customer);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/password")
    @Transactional
    public ResponseEntity<?> resetPassword(@PathVariable("id") Long id, @RequestBody PasswordPayload payload) {
        if (payload == null || payload.getPassword() == null || payload.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "密码不能为空"));
        }
        if (payload.getPassword().length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "密码至少6位"));
        }
        Optional<Customer> opt = customerRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Customer c = opt.get();
        c.setPassword(payload.getPassword());
        customerRepository.save(c);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    public static class StatusPayload {
        private String status; // ACTIVE, DISABLED

        public StatusPayload() {
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class PasswordPayload {
        private String password;

        public PasswordPayload() {
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class CreatePayload {
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String status;

        public CreatePayload() {
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class UpdatePayload {
        private String username;
        private String realName;
        private String phone;
        private String status;

        public UpdatePayload() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class CustomerRow {
        private final Long id;
        private final String username;
        private final String realName;
        private final String phone;
        private final String status;
        private final String createdAt;

        public CustomerRow(Long id,
                           String username,
                           String realName,
                           String phone,
                           String status,
                           String createdAt) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.phone = phone;
            this.status = status;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getRealName() {
            return realName;
        }

        public String getPhone() {
            return phone;
        }

        public String getStatus() {
            return status;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    private static String validateCreate(CreatePayload payload) {
        if (payload == null) return "参数不完整";
        if (isBlank(payload.getUsername())) return "用户名不能为空";
        if (isBlank(payload.getPassword())) return "密码不能为空";
        if (payload.getPassword().length() < 6) return "密码至少6位";
        if (isBlank(payload.getRealName())) return "姓名不能为空";
        if (isBlank(payload.getPhone())) return "电话不能为空";
        if (payload.getUsername().trim().length() > 50) return "用户名过长";
        if (payload.getRealName().trim().length() > 50) return "姓名过长";
        if (payload.getPhone().trim().length() > 20) return "电话过长";
        return null;
    }

    private static String validateUpdate(UpdatePayload payload) {
        if (payload == null) return "参数不完整";
        if (payload.getUsername() != null && payload.getUsername().trim().length() > 50) return "用户名过长";
        if (isBlank(payload.getRealName())) return "姓名不能为空";
        if (isBlank(payload.getPhone())) return "电话不能为空";
        if (payload.getRealName().trim().length() > 50) return "姓名过长";
        if (payload.getPhone().trim().length() > 20) return "电话过长";
        return null;
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}
