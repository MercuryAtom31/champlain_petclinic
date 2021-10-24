package com.petclinic.customers.presentationlayer;

import com.petclinic.customers.businesslayer.PetService;
import com.petclinic.customers.datalayer.*;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Maciej Szarlinski
 * Copied from https://github.com/spring-petclinic/spring-petclinic-microservices
 */

@RequestMapping("/owners/{ownerId}/pets")
@RestController
@Timed("petclinic.pet")
@Slf4j
class PetResource {

    private final PetService petService;

    public PetResource(PetService petService) {
        this.petService = petService;
    }

    /**
     * Create Pet
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pet createNewPet(@RequestBody PetRequest petRequest, @PathVariable("ownerId") int ownerId) {
        Pet pet = petService.CreatePet(petRequest, ownerId);
        return pet;
    }

    @GetMapping
    public List<Pet> findAll()
    {
        return petService.findAll();
    }

    //Find Pet
    @GetMapping(value = "/{petId}")
    public PetDetails findPet(@PathVariable("petId") int petId) {
        return new PetDetails(findPetById(petId).get());

        /*
        String ownerInfo = findPetById(petId).get().getOwner().getFirstName() + " " + findPetById(petId).get().getOwner().getFirstName();
        return new PetDetails(findPetById(petId).get().getId(), findPetById(petId).get().getName(), ownerInfo, findPetById(petId).get().getBirthDate(), findPetById(petId).get().getType() );
         */
    }

    @DeleteMapping(value = "/{petId}")
    public void DeletePet(@PathVariable("petId") int petId, @PathVariable("ownerId") int ownerId) {
        //Call external method deletePet() from petService
        petService.deletePet(petId, ownerId);
    }

    private Optional<Pet> findPetById(int petId) {
        return petService.findByPetId(petId);

    }

}
