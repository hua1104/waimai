package com.example.takeout.web;

import com.example.takeout.entity.Customer;
import com.example.takeout.entity.CustomerAddress;
import com.example.takeout.repository.CustomerAddressRepository;
import com.example.takeout.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/addresses")
public class CustomerAddressController {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;

    public CustomerAddressController(CustomerRepository customerRepository,
                                     CustomerAddressRepository addressRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
    }

    @GetMapping
    public ResponseEntity<List<AddressRow>> list(@RequestParam("customerId") Long customerId) {
        if (customerId == null || !customerRepository.existsById(customerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        List<AddressRow> rows = addressRepository.findByCustomer_IdOrderByIsDefaultDescIdDesc(customerId).stream()
                .map(CustomerAddressController::toRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/default")
    public ResponseEntity<AddressRow> getDefault(@RequestParam("customerId") Long customerId) {
        if (customerId == null || !customerRepository.existsById(customerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<CustomerAddress> opt = addressRepository.findFirstByCustomer_IdAndIsDefaultTrue(customerId);
        return opt.map(a -> ResponseEntity.ok(toRow(a))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody AddressPayload payload) {
        String err = validate(payload);
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }
        Optional<Customer> customerOpt = customerRepository.findById(payload.getCustomerId());
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "食客不存在"));
        }

        CustomerAddress a = new CustomerAddress();
        a.setCustomer(customerOpt.get());
        a.setLabel(safe(payload.getLabel(), 50));
        a.setAddressDetail(safeRequired(payload.getAddressDetail(), 255));
        a.setContactName(safe(payload.getContactName(), 50));
        a.setContactPhone(safe(payload.getContactPhone(), 20));
        a.setIsDefault(Boolean.TRUE.equals(payload.getIsDefault()));

        if (Boolean.TRUE.equals(a.getIsDefault())) {
            addressRepository.clearDefault(payload.getCustomerId());
        } else {
            boolean hasAny = addressRepository.findByCustomer_IdOrderByIsDefaultDescIdDesc(payload.getCustomerId()).stream().findAny().isPresent();
            if (!hasAny) {
                a.setIsDefault(true);
            }
        }

        CustomerAddress saved = addressRepository.save(a);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRow(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody AddressPayload payload) {
        String err = validate(payload);
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }
        Optional<CustomerAddress> opt = addressRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        CustomerAddress a = opt.get();
        if (a.getCustomer() == null || a.getCustomer().getId() == null || !a.getCustomer().getId().equals(payload.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        a.setLabel(safe(payload.getLabel(), 50));
        a.setAddressDetail(safeRequired(payload.getAddressDetail(), 255));
        a.setContactName(safe(payload.getContactName(), 50));
        a.setContactPhone(safe(payload.getContactPhone(), 20));

        if (Boolean.TRUE.equals(payload.getIsDefault())) {
            addressRepository.clearDefault(payload.getCustomerId());
            a.setIsDefault(true);
        }

        CustomerAddress saved = addressRepository.save(a);
        return ResponseEntity.ok(toRow(saved));
    }

    @PostMapping("/{id}/default")
    @Transactional
    public ResponseEntity<?> setDefault(@PathVariable("id") Long id, @RequestParam("customerId") Long customerId) {
        if (customerId == null || !customerRepository.existsById(customerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<CustomerAddress> opt = addressRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        CustomerAddress a = opt.get();
        if (a.getCustomer() == null || a.getCustomer().getId() == null || !a.getCustomer().getId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        addressRepository.clearDefault(customerId);
        a.setIsDefault(true);
        addressRepository.save(a);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable("id") Long id, @RequestParam("customerId") Long customerId) {
        if (customerId == null || !customerRepository.existsById(customerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<CustomerAddress> opt = addressRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        CustomerAddress a = opt.get();
        if (a.getCustomer() == null || a.getCustomer().getId() == null || !a.getCustomer().getId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean wasDefault = Boolean.TRUE.equals(a.getIsDefault());
        addressRepository.deleteById(id);
        if (wasDefault) {
            addressRepository.findByCustomer_IdOrderByIsDefaultDescIdDesc(customerId).stream().findFirst().ifPresent(next -> {
                addressRepository.clearDefault(customerId);
                next.setIsDefault(true);
                addressRepository.save(next);
            });
        }
        return ResponseEntity.noContent().build();
    }

    private static AddressRow toRow(CustomerAddress a) {
        return new AddressRow(
                a.getId(),
                a.getLabel(),
                a.getAddressDetail(),
                a.getContactName(),
                a.getContactPhone(),
                Boolean.TRUE.equals(a.getIsDefault()),
                a.getCreatedAt() == null ? null : a.getCreatedAt().toString()
        );
    }

    private static String validate(AddressPayload payload) {
        if (payload == null || payload.getCustomerId() == null) return "参数不完整";
        if (payload.getAddressDetail() == null || payload.getAddressDetail().trim().isEmpty()) return "地址不能为空";
        return null;
    }

    private static String safe(String v, int maxLen) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    private static String safeRequired(String v, int maxLen) {
        String t = v == null ? "" : v.trim();
        if (t.length() > maxLen) return t.substring(0, maxLen);
        return t;
    }

    public static class AddressPayload {
        private Long customerId;
        private String label;
        private String addressDetail;
        private String contactName;
        private String contactPhone;
        private Boolean isDefault;

        public AddressPayload() {
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getAddressDetail() {
            return addressDetail;
        }

        public void setAddressDetail(String addressDetail) {
            this.addressDetail = addressDetail;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public Boolean getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(Boolean isDefault) {
            this.isDefault = isDefault;
        }
    }

    public static class AddressRow {
        private Long id;
        private String label;
        private String addressDetail;
        private String contactName;
        private String contactPhone;
        private boolean isDefault;
        private String createdAt;

        public AddressRow(Long id,
                          String label,
                          String addressDetail,
                          String contactName,
                          String contactPhone,
                          boolean isDefault,
                          String createdAt) {
            this.id = id;
            this.label = label;
            this.addressDetail = addressDetail;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
            this.isDefault = isDefault;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getAddressDetail() {
            return addressDetail;
        }

        public String getContactName() {
            return contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public boolean getIsDefault() {
            return isDefault;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }
}
