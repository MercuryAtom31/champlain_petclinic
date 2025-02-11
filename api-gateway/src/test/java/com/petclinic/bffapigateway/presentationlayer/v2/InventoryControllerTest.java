package com.petclinic.bffapigateway.presentationlayer.v2;

import com.petclinic.bffapigateway.domainclientlayer.InventoryServiceClient;
import com.petclinic.bffapigateway.dtos.Inventory.*;
import com.petclinic.bffapigateway.exceptions.InventoryNotFoundException;
import com.petclinic.bffapigateway.utils.InventoryUtils.ImageUtil;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.webjars.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        InventoryController.class,
        InventoryServiceClient.class
})
@WebFluxTest(controllers = InventoryController.class)
@AutoConfigureWebTestClient
public class InventoryControllerTest {
    @Autowired
    private WebTestClient client;
    @MockBean
    private InventoryServiceClient inventoryServiceClient;
    private final String baseInventoryURL = "/api/v2/gateway/inventories";

    InputStream inputStream = getClass().getResourceAsStream("/images/DiagnosticKitImage.jpg");
    byte[] diagnosticKitImage = ImageUtil.readImage(inputStream);

    public InventoryControllerTest() throws IOException {
    }

    private InventoryResponseDTO buildInventoryDTO(){
        return InventoryResponseDTO.builder()
                .inventoryId("1")
                .inventoryName("invt1")
                .inventoryType("Internal")
                .inventoryDescription("invtone")
                .inventoryImage("https://www.fda.gov/files/iStock-157317886.jpg")
                .inventoryBackupImage("https://www.who.int/images/default-source/wpro/countries/viet-nam/health-topics/vaccines.jpg?sfvrsn=89a81d7f_14")
                .imageUploaded(diagnosticKitImage)
                .build();
    }
    private List<InventoryTypeResponseDTO> buildInventoryTypeResponseDTOList(){
        return List.of(InventoryTypeResponseDTO.builder()
                .type("Internal")
                .typeId("1")
                .build(), InventoryTypeResponseDTO.builder().typeId("2").type("External").build());
    }

    @Test
    void deleteAllInventories_shouldSucceed() {
        // Arrange
        when(inventoryServiceClient.deleteAllInventories())
                .thenReturn(Mono.empty());  // Using Mono.empty() to simulate a void return (successful deletion without a return value).

        // Act
        client.delete()
                .uri("/api/v2/gateway/inventories")  // Assuming the endpoint for deleting all inventories is the same without an ID.
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        // Assert
        verify(inventoryServiceClient, times(1))
                .deleteAllInventories();
    }


    @Test
    void getAllInventories_withValidPageSize_and_PageNumber_shouldReturnInventories() {
        //Arrange
        Optional<Integer> page = Optional.of(0);
        Optional<Integer> size = Optional.of(2);
        when(inventoryServiceClient.searchInventory(page, size, null, null, null))
                .thenReturn(Flux.just(buildInventoryDTO()));

        // Act
        client.get()
                .uri(baseInventoryURL + "?page=0&size=2")
                .accept(MediaType.valueOf(MediaType.TEXT_EVENT_STREAM_VALUE))
                .acceptCharset(StandardCharsets.UTF_8)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InventoryResponseDTO.class)
                .hasSize(1)
                .contains(buildInventoryDTO());

        // Assert
        verify(inventoryServiceClient, times(1))
                .searchInventory(eq(page), eq(size), eq(null), eq(null), eq(null));
    }

    @Test
    void getAllInventories_with_ValidQueryParams_InventoryName_InventoryType_InventoryDescription_shouldReturnOneInventory() {
        // Arrange
        Optional<Integer> page = Optional.of(0);
        Optional<Integer> size = Optional.of(2);
        when(inventoryServiceClient.searchInventory(page, size, "invt1", "Internal", "invtone"))
                .thenReturn(Flux.just(buildInventoryDTO()));

        // Act
        client.get()
                .uri(baseInventoryURL + "?page=0&size=2&inventoryName=invt1&inventoryType=Internal&inventoryDescription=invtone")
                .accept(MediaType.valueOf(MediaType.TEXT_EVENT_STREAM_VALUE))
                .acceptCharset(StandardCharsets.UTF_8)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InventoryResponseDTO.class)
                .hasSize(1)
                .contains(buildInventoryDTO());

        // Assert
        verify(inventoryServiceClient, times(1))
                .searchInventory(eq(page), eq(size), eq("invt1"), eq("Internal"), eq("invtone"));
    }

    @Test
    void getAllInventories_with_Invalid_QueryParams_shouldReturnEmptyList() {
        // Arrange
        Optional<Integer> page = Optional.of(0);
        Optional<Integer> size = Optional.of(2);
        when(inventoryServiceClient.searchInventory(page, size, "invalid", "invalid", "invalid"))
                .thenReturn(Flux.empty());

        // Act
        client.get()
                .uri(baseInventoryURL + "?page=0&size=2&inventoryName=invalid&inventoryType=invalid&inventoryDescription=invalid")
                .accept(MediaType.valueOf(MediaType.TEXT_EVENT_STREAM_VALUE))
                .acceptCharset(StandardCharsets.UTF_8)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InventoryResponseDTO.class)
                .hasSize(0);

        // Assert
        verify(inventoryServiceClient, times(1))
                .searchInventory(eq(page), eq(size), eq("invalid"), eq("invalid"), eq("invalid"));
    }

    @Test
    void getAllInventoryTypes_shouldReturnInventoryTypes() {
        // Arrange
        when(inventoryServiceClient.getAllInventoryTypes())
                .thenReturn(Flux.fromIterable(buildInventoryTypeResponseDTOList()));

        // Act
        client.get()
                .uri(baseInventoryURL + "/types")
                .accept(MediaType.valueOf(MediaType.TEXT_EVENT_STREAM_VALUE))
                .acceptCharset(StandardCharsets.UTF_8)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InventoryTypeResponseDTO.class)
                .hasSize(2);

        // Assert
        verify(inventoryServiceClient, times(1))
                .getAllInventoryTypes();
    }

    @Test
    void deleteInventoryById_WithValidInventoryId_ShouldSucceed() {
        // Arrange
        String inventoryId = "1";
        when(inventoryServiceClient.deleteInventoryByInventoryId(inventoryId))
                .thenReturn(Mono.empty());

        // Act
        client.delete()
                .uri(baseInventoryURL + "/" + inventoryId)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        // Assert
        verify(inventoryServiceClient, times(1))
                .deleteInventoryByInventoryId(eq(inventoryId));
    }

    @Test
    void deleteInventoryById_WithInvalid_ShouldReturnNotFound() {
        // Arrange
        String inventoryId = "invalid";
        when(inventoryServiceClient.deleteInventoryByInventoryId(inventoryId))
                .thenThrow(new NotFoundException("Inventory not found"));

        // Act
        client.delete()
                .uri(baseInventoryURL + "/" + inventoryId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Assert
        verify(inventoryServiceClient, times(1))
                .deleteInventoryByInventoryId(inventoryId);
    }

    @Test
    void getInventoryById_withValidId_shouldReturnInventory() {
        // Arrange
        String inventoryId = "1";
        InventoryResponseDTO inventory = buildInventoryDTO();
        when(inventoryServiceClient.getInventoryById(inventoryId))
                .thenReturn(Mono.just(inventory));

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(InventoryResponseDTO.class)
                .isEqualTo(inventory);

        // Assert
        verify(inventoryServiceClient, times(1))
                .getInventoryById(eq(inventoryId));
    }

    @Test
    void getInventoryById_withInvalidIdFormat_shouldReturnBadRequest() {
        // Arrange
        String invalidInventoryId = "invalid-id-format";
        when(inventoryServiceClient.getInventoryById(invalidInventoryId))
                .thenReturn(Mono.empty());

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + invalidInventoryId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Assert
        verify(inventoryServiceClient, times(1))
                .getInventoryById(eq(invalidInventoryId));
    }

    @Test
    void getInventoryById_withNonExistentId_shouldReturnNotFound() {
        // Arrange
        String nonExistentInventoryId = "non-existent-id";
        when(inventoryServiceClient.getInventoryById(nonExistentInventoryId))
                .thenReturn(Mono.empty());

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + nonExistentInventoryId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Assert
        verify(inventoryServiceClient, times(1))
                .getInventoryById(eq(nonExistentInventoryId));
    }

    @Test
    void updateInventoryById_withValidId_shouldReturnUpdatedInventory() {
        // Arrange
        String inventoryId = "dfa0a7e3-5a40-4b86-881e-9549ecda5e4b";
        InventoryRequestDTO updateRequest = InventoryRequestDTO.builder()
                .inventoryName("updatedName")
                .inventoryType("updatedType")
                .inventoryDescription("updatedDescription")
                .inventoryImage("https://www.fda.gov/files/iStock-157317886.jpg")
                .inventoryBackupImage("https://www.who.int/images/default-source/wpro/countries/viet-nam/health-topics/vaccines.jpg?sfvrsn=89a81d7f_14")
                .imageUploaded(diagnosticKitImage)
                .build();
        InventoryResponseDTO updatedInventory = InventoryResponseDTO.builder()
                .inventoryId(inventoryId)
                .inventoryName("updatedName")
                .inventoryType("updatedType")
                .inventoryDescription("updatedDescription")
                .inventoryImage("https://www.fda.gov/files/iStock-157317886.jpg")
                .inventoryBackupImage("https://www.who.int/images/default-source/wpro/countries/viet-nam/health-topics/vaccines.jpg?sfvrsn=89a81d7f_14")
                .imageUploaded(diagnosticKitImage)
                .build();

        when(inventoryServiceClient.updateInventory(eq(updateRequest), eq(inventoryId)))
                .thenReturn(Mono.just(updatedInventory));

        // Act
        client.put()
                .uri(baseInventoryURL + "/" + inventoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(InventoryResponseDTO.class)
                .isEqualTo(updatedInventory);

        // Assert
        verify(inventoryServiceClient, times(1))
                .updateInventory(eq(updateRequest), eq(inventoryId));
    }

    @Test
    void updateInventoryById_withServiceLayerFailure_shouldReturnInternalServerError() {
        // Arrange
        String validInventoryId = "dfa0a7e3-5a40-4b86-881e-9549ecda5e4b";
        InventoryRequestDTO updateRequest = InventoryRequestDTO.builder()
                .inventoryName("updatedName")
                .inventoryType("updatedType")
                .inventoryDescription("updatedDescription")
                .inventoryImage("https://www.fda.gov/files/iStock-157317886.jpg")
                .inventoryBackupImage("https://www.who.int/images/default-source/wpro/countries/viet-nam/health-topics/vaccines.jpg?sfvrsn=89a81d7f_14")
                .imageUploaded(diagnosticKitImage)
                .build();

        when(inventoryServiceClient.updateInventory(eq(updateRequest), eq(validInventoryId)))
                .thenReturn(Mono.error(new RuntimeException("Service failure")));  // Simulating service failure.

        // Act
        client.put()
                .uri(baseInventoryURL + "/" + validInventoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().is5xxServerError();  // Expecting 500 Internal Server Error.

        // Assert
        verify(inventoryServiceClient, times(1))
                .updateInventory(eq(updateRequest), eq(validInventoryId));
    }

    @Test
    void addInventory_withValidInventoryRequest_shouldReturnCreatedInventory() {
        // Arrange
        InventoryRequestDTO inventoryRequest = InventoryRequestDTO.builder()
                .inventoryName("invt1")
                .inventoryType("Internal")
                .inventoryDescription("invtone")
                .inventoryImage("https://www.fda.gov/files/iStock-157317886.jpg")
                .inventoryBackupImage("https://www.who.int/images/default-source/wpro/countries/viet-nam/health-topics/vaccines.jpg?sfvrsn=89a81d7f_14")
                .imageUploaded(diagnosticKitImage)
                .build();
        InventoryResponseDTO createdInventory = buildInventoryDTO();

        when(inventoryServiceClient.addInventory(eq(inventoryRequest)))
                .thenReturn(Mono.just(createdInventory));

        // Act
        client.post()
                .uri(baseInventoryURL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(inventoryRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(InventoryResponseDTO.class)
                .isEqualTo(createdInventory);

        // Assert
        verify(inventoryServiceClient, times(1))
                .addInventory(eq(inventoryRequest));
    }

    @Test
    void addInventory_withInvalidInventoryRequest_shouldReturnBadRequest() {
        // Arrange
        InventoryRequestDTO invalidInventoryRequest = InventoryRequestDTO.builder()
                .inventoryName("invt1")
                .inventoryType("Internal")
                .inventoryDescription("invtone")
                .inventoryImage("https://www.fda.gov/files/iStock-157317886.jpg")
                .inventoryBackupImage("https://www.who.int/images/default-source/wpro/countries/viet-nam/health-topics/vaccines.jpg?sfvrsn=89a81d7f_14")
                .imageUploaded(diagnosticKitImage)
                .build();

        when(inventoryServiceClient.addInventory(eq(invalidInventoryRequest)))
                .thenReturn(Mono.empty());

        // Act
        client.post()
                .uri(baseInventoryURL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidInventoryRequest)
                .exchange()
                .expectStatus().isBadRequest();

        // Assert
        verify(inventoryServiceClient, times(1))
                .addInventory(eq(invalidInventoryRequest));
    }

    @Test
    void searchProductsByInventoryIdAndProductNameAndProductDescriptionAndStatus_withValidInventoryIdAndProductNameAndProductDescriptionAndStatus_shouldReturnProducts() {
        // Arrange
        String inventoryId = "1";
        String productName = "product1";
        String productDescription = "productone";
        Status status = Status.AVAILABLE;
        ProductResponseDTO product = ProductResponseDTO.builder()
                .productId("1")
                .productName("product1")
                .productDescription("productone")
                .status(Status.AVAILABLE)
                .build();

        when(inventoryServiceClient.searchProducts(inventoryId, productName, productDescription, status))
                .thenReturn(Flux.just(product));

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/search?productName=product1&productDescription=productone&status=AVAILABLE")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponseDTO.class)
                .hasSize(1)
                .contains(product);

        // Assert
        verify(inventoryServiceClient, times(1))
                .searchProducts(eq(inventoryId), eq(productName), eq(productDescription), eq(status));
    }

    @Test
    void searchProductsByInventoryIdAndProductNameAndProductDescriptionAndStatus_withInvalidInventoryId_shouldReturnNoContent() {
        // Arrange
        String invalidInventoryId = "invalid";
        String productName = "product1";
        String productDescription = "productone";
        Status status = Status.AVAILABLE;

        when(inventoryServiceClient.searchProducts(invalidInventoryId, productName, productDescription, status))
                .thenReturn(Flux.empty());

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + invalidInventoryId + "/products/search?productName=product1&productDescription=productone&status=AVAILABLE")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();

        // Assert
        verify(inventoryServiceClient, times(1))
                .searchProducts(eq(invalidInventoryId), eq(productName), eq(productDescription), eq(status));
    }

    @Test
    void searchProductsByInventoryIdAndProductNameAndProductDescription_withValidStatusAndInvalidProductNameAndProductDescription_shouldReturnEmptyList() {
        // Arrange
        String inventoryId = "1";
        String invalidProductName = "invalid";
        String invalidProductDescription = "invalid";
        Status status = Status.AVAILABLE;

        when(inventoryServiceClient.searchProducts(inventoryId, invalidProductName, invalidProductDescription, status))
                .thenReturn(Flux.empty());

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/search?productName=invalid&productDescription=invalid&status=AVAILABLE")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();

        // Assert
        verify(inventoryServiceClient, times(1))
                .searchProducts(eq(inventoryId), eq(invalidProductName), eq(invalidProductDescription), eq(status));
    }

    @Test
    void searchProductsByInventoryIdAndProductNameAndProductDescription_withValidInventoryIdAndInvalidProductNameAndProductDescription_shouldReturnEmptyList() {
        // Arrange
        String inventoryId = "1";
        String invalidProductName = "invalid";
        String invalidProductDescription = "invalid";

        when(inventoryServiceClient.searchProducts(inventoryId, invalidProductName, invalidProductDescription, null))
                .thenReturn(Flux.empty());

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/search?productName=invalid&productDescription=invalid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();

        // Assert
        verify(inventoryServiceClient, times(1))
                .searchProducts(eq(inventoryId), eq(invalidProductName), eq(invalidProductDescription), eq(null));
    }

    @Test
    void searchProductsByInventoryIdAndProductNameAndProductDescription_withValidInventoryIdAndProductNameAndProductDescriptionAndStatus_shouldReturnProducts() {
        // Arrange
        String inventoryId = "1";
        String productName = "product1";
        String productDescription = "productone";
        Status status = Status.AVAILABLE;
        ProductResponseDTO product = ProductResponseDTO.builder()
                .productId("1")
                .productName("product1")
                .productDescription("productone")
                .productPrice(100.0)
                .status(Status.AVAILABLE)
                .build();

        when(inventoryServiceClient.searchProducts(inventoryId, productName, productDescription, status))
                .thenReturn(Flux.just(product));

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/search?productName=product1&productDescription=productone&status=AVAILABLE")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponseDTO.class)
                .hasSize(1)
                .contains(product);
        // Assert
        verify(inventoryServiceClient, times(1))
                .searchProducts(eq(inventoryId), eq(productName), eq(productDescription), eq(status));
    }

    @Test
    void getProductByProductIdInInventory_withValidInventoryIdAndProductId_shouldReturnProduct() {
        // Arrange
        String inventoryId = "1";
        String productId = "101";
        ProductResponseDTO productResponseDTO = ProductResponseDTO.builder()
                .productId(productId)
                .productName("Product 101")
                .productDescription("Description of Product 101")
                .productPrice(99.99)
                .build();

        when(inventoryServiceClient.getProductByProductIdInInventory(inventoryId, productId))
                .thenReturn(Mono.just(productResponseDTO));

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/" + productId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDTO.class)
                .isEqualTo(productResponseDTO);

        // Assert
        verify(inventoryServiceClient, times(1))
                .getProductByProductIdInInventory(inventoryId, productId);
    }

    @Test
    void getProductByProductIdInInventory_withInvalidProductId_shouldReturnNotFound() {
        // Arrange
        String inventoryId = "1";
        String invalidProductId = "999";
        when(inventoryServiceClient.getProductByProductIdInInventory(inventoryId, invalidProductId))
                .thenReturn(Mono.empty());

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/" + invalidProductId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Assert
        verify(inventoryServiceClient, times(1))
                .getProductByProductIdInInventory(inventoryId, invalidProductId);
    }

    @Test
    void updateProductInInventory_withValidData_shouldReturnUpdatedProduct() {
        // Arrange
        String inventoryId = "1";
        String productId = "101";
        ProductRequestDTO productRequestDTO = ProductRequestDTO.builder()
                .productName("Updated Product 101")
                .productDescription("Updated Description")
                .productPrice(149.99)
                .build();

        ProductResponseDTO updatedProductResponseDTO = ProductResponseDTO.builder()
                .productId(productId)
                .productName("Updated Product 101")
                .productDescription("Updated Description")
                .productPrice(149.99)
                .build();

        when(inventoryServiceClient.updateProductInInventory(productRequestDTO, inventoryId, productId))
                .thenReturn(Mono.just(updatedProductResponseDTO));

        // Act
        client.put()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productRequestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDTO.class)
                .isEqualTo(updatedProductResponseDTO);

        // Assert
        verify(inventoryServiceClient, times(1))
                .updateProductInInventory(productRequestDTO, inventoryId, productId);
    }

    @Test
    void updateProductInInventory_withInvalidData_shouldReturnBadRequest() {
        // Arrange
        String inventoryId = "1";
        String productId = "101";
        ProductRequestDTO invalidProductRequestDTO = ProductRequestDTO.builder()
                .productName("")
                .productDescription("")
                .productPrice(-10.0)
                .build();

        when(inventoryServiceClient.updateProductInInventory(invalidProductRequestDTO, inventoryId, productId))
                .thenReturn(Mono.empty());

        // Act
        client.put()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidProductRequestDTO)
                .exchange()
                .expectStatus().isBadRequest();

        // Assert
        verify(inventoryServiceClient, times(1))
                .updateProductInInventory(invalidProductRequestDTO, inventoryId, productId);
    }

    @Test
    void getQuantityOfProductsInInventory_withValidInventoryId_shouldReturnProductQuantity() {
        // Arrange
        String inventoryId = "1";
        Integer expectedQuantity = 100;  // Simulating that there are 100 products in the inventory

        // Mocking the service to return the expected quantity
        when(inventoryServiceClient.getQuantityOfProductsInInventory(inventoryId))
                .thenReturn(Mono.just(expectedQuantity));

        // Act and Assert
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/productquantity")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .value(quantity -> {
                    assertNotNull(quantity);
                    assertEquals(expectedQuantity, quantity);
                });

        // Verify that the service was called with the correct inventoryId
        verify(inventoryServiceClient, times(1))
                .getQuantityOfProductsInInventory(eq(inventoryId));
    }
    
    @Test
    void getQuantityOfProductsInInventory_withServerError_shouldReturnInternalServerError() {
        // Arrange
        String inventoryId = "1";
        String errorMessage = "Internal Server Error";

        // Mocking the service to throw an error (simulating a 500 error from the server)
        when(inventoryServiceClient.getQuantityOfProductsInInventory(inventoryId))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        // Act and Assert
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/productquantity")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Internal Server Error");

        // Verify that the service was called with the correct inventoryId
        verify(inventoryServiceClient, times(1))
                .getQuantityOfProductsInInventory(eq(inventoryId));
    }

    @Test
    void addSupplyToInventory_withInvalidRequest_shouldReturnBadRequest() {
        // Arrange
        ProductRequestDTO invalidProductRequestDTO = ProductRequestDTO.builder()
                .productName("")
                .productDescription("")
                .productPrice(-10.0)
                .build();

        String inventoryId = "1"; // Define the inventoryId

        when(inventoryServiceClient.addSupplyToInventory(invalidProductRequestDTO, inventoryId))
                .thenReturn(Mono.empty());

        // Act
        client.post()
                .uri(baseInventoryURL + "/" + inventoryId + "/products") // Use the inventoryId variable
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidProductRequestDTO)
                .exchange()
                .expectStatus().isBadRequest();

        // Assert
        verify(inventoryServiceClient, times(1))
                .addSupplyToInventory(invalidProductRequestDTO, inventoryId);
    }

    @Test
    void addSupplyToInventory_withValidRequest_shouldReturnCreatedProduct() {
        // Arrange
        ProductRequestDTO productRequestDTO = ProductRequestDTO.builder()
                .productName("Product 101")
                .productDescription("Description of Product 101")
                .productPrice(99.99)
                .build();

        String inventoryId = "1"; // Define the inventoryId

        ProductResponseDTO createdProductResponseDTO = ProductResponseDTO.builder()
                .productId("101")
                .productName("Product 101")
                .productDescription("Description of Product 101")
                .productPrice(99.99)
                .build();

        when(inventoryServiceClient.addSupplyToInventory(productRequestDTO, inventoryId))
                .thenReturn(Mono.just(createdProductResponseDTO));

        // Act
        client.post()
                .uri(baseInventoryURL + "/" + inventoryId + "/products") // Use the inventoryId variable
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productRequestDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponseDTO.class)
                .isEqualTo(createdProductResponseDTO);

        // Assert
        verify(inventoryServiceClient, times(1))
                .addSupplyToInventory(productRequestDTO, inventoryId);
    }


    @Test
    void consumeProduct_withValidData_shouldReturnUpdatedProduct() {
        // Arrange
        String inventoryId = "1";
        String productId = "101";
        ProductRequestDTO productRequestDTO = ProductRequestDTO.builder()
                .productName("Product 101")
                .productDescription("Description of Product 101")
                .productPrice(99.99)
                .build();

        ProductResponseDTO updatedProductResponseDTO = ProductResponseDTO.builder()
                .productId(productId)
                .productName("Product 101")
                .productDescription("Description of Product 101")
                .productPrice(99.99)
                .build();

        when(inventoryServiceClient.consumeProduct(inventoryId, productId))
                .thenReturn(Mono.just(updatedProductResponseDTO));

        // Act
        client.patch()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/" + productId + "/consume")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productRequestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDTO.class)
                .isEqualTo(updatedProductResponseDTO);

        // Assert
        verify(inventoryServiceClient, times(1))
                .consumeProduct(inventoryId, productId);
    }

    @Test
    void consumeProduct_withInvalidData_shouldReturnNotFound() {
        // Arrange
        String inventoryId = "1";
        String invalidProductId = "999";
        when(inventoryServiceClient.consumeProduct(inventoryId, invalidProductId))
                .thenReturn(Mono.empty());

        // Act
        client.patch()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/" + invalidProductId + "/consume")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Assert
        verify(inventoryServiceClient, times(1))
                .consumeProduct(inventoryId, invalidProductId);
    }

    @Test
    void deleteAllProductsInInventory_ShouldSucceed() {

        String inventoryId = "inventory1";
        // Mock the service call to simulate the successful deletion of all products in an inventory.
        // Assuming your service client has a method called `deleteAllProductsInInventory`.
        when(inventoryServiceClient.deleteAllProductsInInventory(inventoryId))
                .thenReturn(Mono.empty());  // Using Mono.empty() to simulate a void return (successful deletion without a return value).

        // Make the DELETE request to the API.
        client.delete()
                .uri("/api/v2/gateway/inventories/" + inventoryId +"/products")  // Assuming the endpoint for deleting all products in an inventory is the same with the inventory ID.
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        // Verify that the deleteAllProductsInInventory method on the service client was called exactly once.
        verify(inventoryServiceClient, times(1))
                .deleteAllProductsInInventory(inventoryId);
    }

    @Test
    public void testCreateSupplyPdf_NotFound() {
        client.get()
                .uri("/inventory/validInventoryId/products/download")
                .exchange()
                .expectStatus().isNotFound();

        // Verify the service method was not called
        verify(inventoryServiceClient, never()).createSupplyPdf(anyString());
    }
    @Test
    public void testCreateSupplyPdf_Success() {
        // Arrange
        String inventoryId = "inventory1";
        byte[] pdfContent = "PDF Content".getBytes();

        // Mock the service method to return the PDF content
        when(inventoryServiceClient.createSupplyPdf(inventoryId))
                .thenReturn(Mono.just(pdfContent));

        // Act
        client.get()
                .uri(baseInventoryURL + "/" + inventoryId + "/products/download")
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .isEqualTo(pdfContent);

        // Verify that the service method was called with the correct inventoryId
        verify(inventoryServiceClient, times(1))
                .createSupplyPdf(eq(inventoryId));
    }

    @Test
    void updateProductInventoryId_withValidData_shouldReturnUpdatedProduct() {
        // Arrange
        String currentInventoryId = "1";
        String productId = "101";
        String newInventoryId = "2";
        ProductResponseDTO updatedProductResponseDTO = ProductResponseDTO.builder()
                .productId(productId)
                .productName("Updated Product 101")
                .productDescription("Updated Description")
                .productPrice(149.99)
                .build();

        when(inventoryServiceClient.updateProductInventoryId(currentInventoryId, productId, newInventoryId))
                .thenReturn(Mono.just(updatedProductResponseDTO));

        // Act
        client.put()
                .uri(baseInventoryURL + "/" + currentInventoryId + "/products/" + productId + "/updateInventoryId/" + newInventoryId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDTO.class)
                .isEqualTo(updatedProductResponseDTO);

        // Assert
        verify(inventoryServiceClient, times(1))
                .updateProductInventoryId(currentInventoryId, productId, newInventoryId);
    }

    @Test
    void updateProductInventoryId_withInvalidData_shouldReturnNotFound() {
        // Arrange
        String currentInventoryId = "1";
        String productId = "999"; // Assuming this product doesn't exist
        String newInventoryId = "2";
        when(inventoryServiceClient.updateProductInventoryId(currentInventoryId, productId, newInventoryId))
                .thenReturn(Mono.empty());

        // Act
        client.put()
                .uri(baseInventoryURL + "/" + currentInventoryId + "/products/" + productId + "/updateInventoryId/" + newInventoryId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Assert
        verify(inventoryServiceClient, times(1))
                .updateProductInventoryId(currentInventoryId, productId, newInventoryId);
    }

    @Test
    void getAllInventories_shouldReturnAllInventories() {
        // Arrange
        InventoryResponseDTO inventoryResponseDTO1 = buildInventoryDTO();
        InventoryResponseDTO inventoryResponseDTO2 = buildInventoryDTO().builder()
                .inventoryId("2")
                .inventoryName("invt2")
                .build();

        when(inventoryServiceClient.getAllInventories())
                .thenReturn(Flux.just(inventoryResponseDTO1, inventoryResponseDTO2));

        // Act
        client.get()
                .uri(baseInventoryURL + "/all")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InventoryResponseDTO.class)
                .hasSize(2)
                .contains(inventoryResponseDTO1, inventoryResponseDTO2);

        // Assert
        verify(inventoryServiceClient, times(1)).getAllInventories();
    }

    @Test
    void restockLowStockProduct_withValidRequest_ShouldSucceed() {
        // Arrange
        String inventoryId = "inventory1";
        String productId = "product1";
        Integer productQuantity = 10;

        ProductResponseDTO restockedProduct = ProductResponseDTO.builder()
                .productId(productId)
                .inventoryId(inventoryId)
                .productName("Restocked Product")
                .productDescription("Restocked Product Description")
                .productPrice(100.0)
                .productQuantity(productQuantity)
                .build();

        when(inventoryServiceClient.restockLowStockProduct(inventoryId, productId, productQuantity))
                .thenReturn(Mono.just(restockedProduct));

        // Act and Assert
        client.put()
                .uri("/api/v2/gateway/inventories/" + inventoryId + "/products/" + productId + "/restockProduct?productQuantity=" + productQuantity)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDTO.class)
                .isEqualTo(restockedProduct);

        // Verify that the service client was called
        verify(inventoryServiceClient, times(1))
                .restockLowStockProduct(inventoryId, productId, productQuantity);
    }


    @Test
    void restockLowStockProduct_withInvalidQuantity_ShouldReturnBadRequest() {
        // Arrange
        String inventoryId = "inventory1";
        String productId = "product1";
        Integer invalidQuantity = -5;

        // Act and Assert
        client.put()
                .uri("/api/v2/gateway/inventories/" + inventoryId + "/products/" + productId + "/restockProduct?productQuantity=" + invalidQuantity)
                .exchange()
                .expectStatus().isBadRequest();

        // Verify that the service client was not called
        verify(inventoryServiceClient, never())
                .restockLowStockProduct(inventoryId, productId, invalidQuantity);
    }

    @Test
    void restockLowStockProduct_withNullProductQuantity_ShouldReturnBadRequest() {
        // Arrange
        String inventoryId = "inventory1";
        String productId = "product1";
        Integer productQuantity = null; // Simulating a null quantity

        // Act and Assert
        client.put()
                .uri("/api/v2/gateway/inventories/" + inventoryId + "/products/" + productId + "/restockProduct")
                .exchange()
                .expectStatus().isBadRequest(); // Expecting 400 Bad Request

        // Verify that the service client was never called
        verify(inventoryServiceClient, never())
                .restockLowStockProduct(eq(inventoryId), eq(productId), eq(productQuantity));
    }

    @Test
    void restockLowStockProduct_withNegativeProductQuantity_ShouldReturnBadRequest() {
        // Arrange
        String inventoryId = "inventory1";
        String productId = "product1";
        Integer productQuantity = -5; // Simulating a negative quantity

        // Act and Assert
        client.put()
                .uri("/api/v2/gateway/inventories/" + inventoryId + "/products/" + productId + "/restockProduct?productQuantity=" + productQuantity)
                .exchange()
                .expectStatus().isBadRequest(); // Expecting 400 Bad Request

        // Verify that the service client was never called
        verify(inventoryServiceClient, never())
                .restockLowStockProduct(eq(inventoryId), eq(productId), eq(productQuantity));
    }

    @Test
    void restockLowStockProduct_withZeroProductQuantity_ShouldReturnBadRequest() {
        // Arrange
        String inventoryId = "inventory1";
        String productId = "product1";
        Integer productQuantity = 0; // Simulating zero quantity

        // Act and Assert
        client.put()
                .uri("/api/v2/gateway/inventories/" + inventoryId + "/products/" + productId + "/restockProduct?productQuantity=" + productQuantity)
                .exchange()
                .expectStatus().isBadRequest(); // Expecting 400 Bad Request

        // Verify that the service client was never called
        verify(inventoryServiceClient, never())
                .restockLowStockProduct(eq(inventoryId), eq(productId), eq(productQuantity));
    }

}