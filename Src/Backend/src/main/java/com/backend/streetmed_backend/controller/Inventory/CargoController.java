package com.backend.streetmed_backend.controller.Inventory;

import com.backend.streetmed_backend.entity.CargoItem;
import com.backend.streetmed_backend.service.cargoService.CargoItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Cargo Management", description = "APIs for managing cargo items and inventory")
@RestController
@RequestMapping("/api/cargo")
public class CargoController {
    private final CargoItemService cargoItemService;
    private final Executor asyncExecutor;

    @Autowired
    public CargoController(CargoItemService cargoItemService,
                           @Qualifier("authExecutor") Executor asyncExecutor) {
        this.cargoItemService = cargoItemService;
        this.asyncExecutor = asyncExecutor;
    }

    @Operation(summary = "Add new cargo item")
    @PostMapping("/items")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> addItem(
            @RequestPart("data") CargoItem item,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    return ResponseEntity.status(401).body(Map.of(
                            "status", "error",
                            "message", "Not authenticated"
                    ));
                }

                CargoItem savedItem = cargoItemService.createItem(item, image);

                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Item added successfully",
                        "itemId", savedItem.getId()
                ));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", e.getMessage()
                ));
            }
        }, asyncExecutor);
    }

    @Operation(summary = "Update cargo item")
    @PutMapping("/items/{id}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateItem(
            @PathVariable Integer id,
            @RequestBody CargoItem item,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Not authenticated"));
                }

                CargoItem updatedItem = cargoItemService.updateItem(id, item, image);

                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Item updated successfully",
                        "item", updatedItem
                ));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()));
            }
        }, asyncExecutor);
    }

    @Operation(summary = "Get all cargo items")
    @GetMapping("/items")
    public CompletableFuture<ResponseEntity<List<CargoItem>>> getAllItems() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(cargoItemService.getAllItems()), asyncExecutor);
    }

    @Operation(summary = "Get low stock items")
    @GetMapping("/items/low-stock")
    public CompletableFuture<ResponseEntity<List<CargoItem>>> getLowStockItems(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus) {

        return CompletableFuture.supplyAsync(() -> {
            if (!"true".equals(authStatus)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok(cargoItemService.getLowStockItems());
        }, asyncExecutor);
    }

}