package com.petclinic.vet.servicelayer;

import com.petclinic.vet.dataaccesslayer.Photo;
import com.petclinic.vet.dataaccesslayer.PhotoRepository;
import com.petclinic.vet.exceptions.InvalidInputException;
import com.petclinic.vet.presentationlayer.PhotoResponseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.r2dbc.init.R2dbcScriptDatabaseInitializer;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class PhotoServiceImplTest {
    @Autowired
    PhotoService photoService;

    @MockBean
    PhotoRepository photoRepository;

    //To counter missing bean error
    @MockBean
    ConnectionFactoryInitializer connectionFactoryInitializer;
    @MockBean
    R2dbcScriptDatabaseInitializer r2dbcScriptDatabaseInitializer;

    String VET_ID = "6748786";
    byte[] photoData = {123, 23, 75, 34};
    Photo photo = Photo.builder()
            .vetId(VET_ID)
            .filename("vet_default.jpg")
            .imgType("image/jpeg")
            .data(photoData)
            .build();

    @Test
    void getPhotoByValidVetId() {
        when(photoRepository.findByVetId(anyString())).thenReturn(Mono.just(photo));

        Mono<Resource> photoMono = photoService.getPhotoByVetId(VET_ID);

        StepVerifier
                .create(photoMono)
                .consumeNextWith(image -> {
                    assertNotNull(image);

                    Resource photo = photoMono.block();
                    assertEquals(photo, image);
                })
                .verifyComplete();
    }
    @Test
    void getDefaultPhotoByValidVetId() {
        String photoName = "vet_default.jpg";
        Photo savedDefaultPhoto = new Photo();
        savedDefaultPhoto.setVetId(VET_ID);
        savedDefaultPhoto.setFilename(photoName);
        savedDefaultPhoto.setImgType("image/jpeg");
        savedDefaultPhoto.setData(photoData);
        when(photoRepository.save(any(Photo.class))).thenReturn(Mono.just(savedDefaultPhoto));

        when(photoRepository.findByVetId(anyString())).thenReturn(Mono.just(photo));

        Mono<PhotoResponseDTO> defaultPhotoMono = photoService.getDefaultPhotoByVetId(VET_ID);

        StepVerifier
                .create(defaultPhotoMono)
                .consumeNextWith(image -> {
                    assertNotNull(image);

                    PhotoResponseDTO photo = defaultPhotoMono.block();
                    assertEquals(photo.getVetId(), image.getVetId());
                })
                .verifyComplete();
    }

    @Test
    void insertPhotoOfVet() {
        String photoName = "vet_default.jpg";
        Mono<Resource> photoResource = Mono.just(new ByteArrayResource(photoData));
        Photo savedPhoto = new Photo();
        savedPhoto.setVetId(VET_ID);
        savedPhoto.setFilename(photoName);
        savedPhoto.setImgType("image/jpeg");
        savedPhoto.setData(photoData);
        when(photoRepository.save(any(Photo.class))).thenReturn(Mono.just(savedPhoto));

        Mono<Resource> savedPhotoMono = photoService.insertPhotoOfVet(VET_ID, photoName, photoResource);

        StepVerifier
                .create(savedPhotoMono)
                .consumeNextWith(image -> {
                    assertNotNull(image);

                    Resource photo = savedPhotoMono.block();
                    assertEquals(photo, image);
                })
                .verifyComplete();
    }

    @Test
    void updatePhotoOfVet() {
        String photoName = "vet_default.jpg";
        Mono<Resource> photoResource = Mono.just(new ByteArrayResource(photoData));
        Photo savedPhoto = new Photo();
        savedPhoto.setVetId(VET_ID);
        savedPhoto.setFilename(photoName);
        savedPhoto.setImgType("image/jpeg");
        savedPhoto.setData(photoData);

        when(photoRepository.save(any(Photo.class))).thenReturn(Mono.just(savedPhoto));
        when(photoRepository.findByVetId(anyString())).thenReturn(Mono.just(savedPhoto));

        Mono<Resource> savedPhotoMono = photoService.updatePhotoByVetId(VET_ID, photoName, photoResource);

        StepVerifier
                .create(savedPhotoMono)
                .consumeNextWith(image -> {
                    assertNotNull(image);

                    Resource photo = savedPhotoMono.block();
                    assertEquals(photo, image);
                })
                .verifyComplete();
    }

    @Test
    void deletePhotoByVetId_PhotoExists_Success() {
        // Arrange
        String vetId = VET_ID;

        // Mock photoRepository.findByVetId(vetId) to return Mono.just(photo)
        when(photoRepository.findByVetId(vetId)).thenReturn(Mono.just(photo));

        // Mock photoRepository.deleteByVetId(vetId) to return Mono.just(1)
        when(photoRepository.deleteByVetId(vetId)).thenReturn(Mono.just(1)); // Assuming 1 row deleted

        // Mock photoRepository.save(...) in insertDefaultPhoto
        when(photoRepository.save(any(Photo.class))).thenReturn(Mono.just(photo));

        // Act
        Mono<Void> result = photoService.deletePhotoByVetId(vetId);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(photoRepository, times(1)).findByVetId(vetId);
        verify(photoRepository, times(1)).deleteByVetId(vetId);
        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    @Test
    void deletePhotoByVetId_PhotoDoesNotExist_ThrowsException() {
        // Arrange
        String vetId = VET_ID;

        // Mock photoRepository.findByVetId(vetId) to return Mono.empty()
        when(photoRepository.findByVetId(vetId)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = photoService.deletePhotoByVetId(vetId);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof InvalidInputException &&
                        throwable.getMessage().equals("Photo not found for vetId: " + vetId))
                .verify();

        verify(photoRepository, times(1)).findByVetId(vetId);
        verify(photoRepository, never()).deleteByVetId(anyString());
        verify(photoRepository, never()).save(any(Photo.class));
    }

}