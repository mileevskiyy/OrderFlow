package com.orderflow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class Contracts {
 private Contracts(){}
 public record ProductInput(@NotBlank @Size(max=40) String sku,@NotBlank @Size(max=140) String name,@NotBlank @Size(max=80) String category,@Min(1) @Max(100000000) long priceCents){}
 public record WarehouseInput(@NotBlank @Size(max=100) String name,@NotBlank @Size(max=100) String location){}
 public record StockInput(@NotBlank String warehouseId,@NotBlank String productId,@Min(1) @Max(100000) int quantity,@NotBlank @Size(max=1000) String reason){}
 public record LineInput(@NotBlank String productId,@Min(1) @Max(10000) int quantity){}
 public record OrderInput(@NotBlank @Size(max=140) String customer,@NotBlank String warehouseId,@NotEmpty @Size(max=50) List<@Valid LineInput> lines){}
 public record PaymentInput(@NotBlank @Size(max=100) String eventId,@NotBlank String orderId,@Min(1) long amountCents,@Pattern(regexp="SUCCEEDED|FAILED") @NotNull String outcome){}
 public record ReturnLineInput(@NotBlank String lineId,@Min(1) @Max(10000) int quantity){}
 public record ReturnInput(@NotBlank @Size(max=1000) String reason,@NotEmpty @Size(max=50) List<@Valid ReturnLineInput> lines){}
}
