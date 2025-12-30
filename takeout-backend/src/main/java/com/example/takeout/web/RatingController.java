package com.example.takeout.web;

import com.example.takeout.entity.*;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.CustomerRepository;
import com.example.takeout.repository.DeliveryRatingRepository;
import com.example.takeout.repository.RestaurantRatingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/ratings")
public class RatingController {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final RestaurantRatingRepository restaurantRatingRepository;
    private final DeliveryRatingRepository deliveryRatingRepository;

    public RatingController(CustomerRepository customerRepository,
                            CustomerOrderRepository customerOrderRepository,
                            RestaurantRatingRepository restaurantRatingRepository,
                            DeliveryRatingRepository deliveryRatingRepository) {
        this.customerRepository = customerRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.restaurantRatingRepository = restaurantRatingRepository;
        this.deliveryRatingRepository = deliveryRatingRepository;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> ratingStatus(@PathVariable("orderId") Long orderId,
                                          @RequestParam("customerId") Long customerId) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CustomerOrder order = orderOpt.get();
        if (order.getCustomer() == null || order.getCustomer().getId() == null || !order.getCustomer().getId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean restaurantRated = restaurantRatingRepository.existsByOrder_Id(orderId);
        boolean deliveryRated = deliveryRatingRepository.existsByOrder_Id(orderId);
        boolean hasDeliveryStaff = order.getDeliveryStaff() != null && order.getDeliveryStaff().getId() != null;

        return ResponseEntity.ok(Map.of(
                "restaurantRated", restaurantRated,
                "deliveryRated", deliveryRated,
                "hasDeliveryStaff", hasDeliveryStaff
        ));
    }

    @PostMapping("/restaurant")
    @Transactional
    public ResponseEntity<?> rateRestaurant(@RequestBody RestaurantRatingRequest request) {
        String err = validateBase(request == null ? null : request.getCustomerId(),
                request == null ? null : request.getOrderId(),
                request == null ? null : request.getScore());
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }

        Optional<Customer> customerOpt = customerRepository.findById(request.getCustomerId());
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "食客不存在"));
        }

        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(request.getOrderId());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "订单不存在"));
        }

        CustomerOrder order = orderOpt.get();
        if (order.getCustomer() == null || order.getCustomer().getId() == null || !order.getCustomer().getId().equals(request.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "订单未完成，暂不可评价"));
        }
        if (restaurantRatingRepository.existsByOrder_Id(order.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该订单已评价过饭店"));
        }

        RestaurantRating rating = new RestaurantRating();
        rating.setOrder(order);
        rating.setCustomer(customerOpt.get());
        rating.setRestaurant(order.getRestaurant());
        rating.setScore(request.getScore());
        rating.setContent(safeText(request.getContent()));
        rating.setCreatedAt(LocalDateTime.now());

        RestaurantRating saved = restaurantRatingRepository.save(rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "status", "OK"));
    }

    @PostMapping("/delivery")
    @Transactional
    public ResponseEntity<?> rateDelivery(@RequestBody DeliveryRatingRequest request) {
        String err = validateBase(request == null ? null : request.getCustomerId(),
                request == null ? null : request.getOrderId(),
                request == null ? null : request.getScore());
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }

        Optional<Customer> customerOpt = customerRepository.findById(request.getCustomerId());
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "食客不存在"));
        }

        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(request.getOrderId());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "订单不存在"));
        }

        CustomerOrder order = orderOpt.get();
        if (order.getCustomer() == null || order.getCustomer().getId() == null || !order.getCustomer().getId().equals(request.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "订单未完成，暂不可评价"));
        }
        if (order.getDeliveryStaff() == null || order.getDeliveryStaff().getId() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该订单未分配骑手，无法评价配送"));
        }
        if (deliveryRatingRepository.existsByOrder_Id(order.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该订单已评价过配送"));
        }

        DeliveryRating rating = new DeliveryRating();
        rating.setOrder(order);
        rating.setCustomer(customerOpt.get());
        rating.setDeliveryStaff(order.getDeliveryStaff());
        rating.setScore(request.getScore());
        rating.setContent(safeText(request.getContent()));
        rating.setCreatedAt(LocalDateTime.now());

        DeliveryRating saved = deliveryRatingRepository.save(rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "status", "OK"));
    }

    private static String validateBase(Long customerId, Long orderId, Integer score) {
        if (customerId == null || orderId == null || score == null) {
            return "参数不完整";
        }
        if (score < 1 || score > 5) {
            return "评分必须在 1~5";
        }
        return null;
    }

    private static String safeText(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (t.length() > 500) return t.substring(0, 500);
        return t;
    }

    public static class RestaurantRatingRequest {
        private Long customerId;
        private Long orderId;
        private Integer score;
        private String content;

        public RestaurantRatingRequest() {
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class DeliveryRatingRequest {
        private Long customerId;
        private Long orderId;
        private Integer score;
        private String content;

        public DeliveryRatingRequest() {
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}

