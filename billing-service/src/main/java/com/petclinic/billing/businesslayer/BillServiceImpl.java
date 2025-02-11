package com.petclinic.billing.businesslayer;

import com.itextpdf.text.DocumentException;
import com.petclinic.billing.datalayer.*;
import com.petclinic.billing.domainclientlayer.OwnerClient;
import com.petclinic.billing.domainclientlayer.VetClient;
import com.petclinic.billing.exceptions.InvalidPaymentException;
import com.petclinic.billing.exceptions.NotFoundException;
import com.petclinic.billing.util.EntityDtoUtil;
import com.petclinic.billing.util.PdfGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Predicate;

import static reactor.core.publisher.FluxExtensionsKt.switchIfEmpty;


@Service
@RequiredArgsConstructor
@Slf4j
public class BillServiceImpl implements BillService{

    private final BillRepository billRepository;
    private final VetClient vetClient;
    private final OwnerClient ownerClient;

    @Override
    public Mono<BillResponseDTO> getBillByBillId(String billUUID) {

        return billRepository.findByBillId(billUUID)
                .doOnNext(bill -> {
                    log.info("Retrieved Bill: {}", bill);
                })
                .map(EntityDtoUtil::toBillResponseDto)
                .doOnNext(t -> t.setTaxedAmount(((t.getAmount() * 15)/100)+ t.getAmount()))
                .doOnNext(t -> t.setTaxedAmount(Math.round(t.getTaxedAmount() * 100.0) / 100.0));
               // .doOnNext(t -> t.setTimeRemaining(timeRemaining(t)));
    }

    @Override
    public Flux<BillResponseDTO> getAllBillsByStatus(BillStatus status) {
        return billRepository.findAllBillsByBillStatus(status).map(EntityDtoUtil::toBillResponseDto);
    }

    @Override
    public Mono<Bill> CreateBillForDB(Mono<Bill> bill) {
        return bill.flatMap(billRepository::insert);
    }

    @Override
    public Flux<BillResponseDTO> getAllBills() {
        return billRepository.findAll()
                .map(EntityDtoUtil::toBillResponseDto);
    }

//    @Override
//    public Flux<BillResponseDTO> getAllBillsByPage(Pageable pageable, String billId, String customerId,
//                                                   String ownerFirstName, String ownerLastName, String visitType,
//                                                   String vetId, String vetFirstName, String vetLastName) {
//        Predicate<Bill> filterCriteria = bill ->
//                (billId == null || bill.getBillId().equals(billId)) &&
//                        (customerId == null || bill.getCustomerId().equals(customerId)) &&
//                        (ownerFirstName == null || bill.getOwnerFirstName().equals(ownerFirstName)) &&
//                        (ownerLastName == null || bill.getOwnerLastName().equals(ownerLastName)) &&
//                        (visitType == null || bill.getVisitType().equals(visitType)) &&
//                        (vetId == null || bill.getVetId().equals(vetId)) &&
//                        (vetFirstName == null || bill.getVetFirstName().equals(vetFirstName)) &&
//                        (vetLastName == null || bill.getVetLastName().equals(vetLastName));
//
//
//        if(billId == null && customerId == null && ownerFirstName == null && ownerLastName == null && visitType == null
//                && vetId == null && vetFirstName == null && vetLastName == null){
//            return billRepository.findAll()
//                    .map(EntityDtoUtil::toBillResponseDto)
//                    .skip(pageable.getPageNumber() * pageable.getPageSize())
//                    .take(pageable.getPageSize());
//        } else {
//            return billRepository.findAll()
//                    .filter(filterCriteria)
//                    .map(EntityDtoUtil::toBillResponseDto)
//                    .skip(pageable.getPageNumber() * pageable.getPageSize())
//                    .take(pageable.getPageSize());
//        }
//    }

    @Override
    public Flux<BillResponseDTO> getAllBillsByPage(Pageable pageable, String billId, String customerId,
                                                   String ownerFirstName, String ownerLastName, String visitType,
                                                   String vetId, String vetFirstName, String vetLastName) {

        Predicate<Bill> filterCriteria = bill ->
                (billId == null || bill.getBillId().equals(billId)) &&
                        (customerId == null || bill.getCustomerId().equals(customerId)) &&
                        (ownerFirstName == null || bill.getOwnerFirstName().equals(ownerFirstName)) &&
                        (ownerLastName == null || bill.getOwnerLastName().equals(ownerLastName)) &&
                        (visitType == null || bill.getVisitType().equals(visitType)) &&
                        (vetId == null || bill.getVetId().equals(vetId)) &&
                        (vetFirstName == null || bill.getVetFirstName().equals(vetFirstName)) &&
                        (vetLastName == null || bill.getVetLastName().equals(vetLastName));

        return billRepository.findAll()
                .filter(filterCriteria)
                .skip(pageable.getPageNumber() * pageable.getPageSize())
                .take(pageable.getPageSize())
                .map(EntityDtoUtil::toBillResponseDto);
    }

    @Override
    public Mono<Long> getNumberOfBillsWithFilters(String billId, String customerId, String ownerFirstName, String ownerLastName,
                                                  String visitType, String vetId, String vetFirstName, String vetLastName) {
        Predicate<Bill> filterCriteria = bill ->
                (billId == null || bill.getBillId().equals(billId)) &&
                        (customerId == null || bill.getCustomerId().equals(customerId)) &&
                        (ownerFirstName == null || bill.getOwnerFirstName().equals(ownerFirstName)) &&
                        (ownerLastName == null || bill.getOwnerLastName().equals(ownerLastName)) &&
                        (visitType == null || bill.getVisitType().equals(visitType)) &&
                        (vetId == null || bill.getVetId().equals(vetId)) &&
                        (vetFirstName == null || bill.getVetFirstName().equals(vetFirstName)) &&
                        (vetLastName == null || bill.getVetLastName().equals(vetLastName));

        return billRepository.findAll()
                .filter(filterCriteria)
                .map(EntityDtoUtil::toBillResponseDto)
                .count();
    }


    @Override
    public Mono<BillResponseDTO> createBill(Mono<BillRequestDTO> billRequestDTO) {

            return billRequestDTO
//                    .map(RequestContextAdd::new)
//                    .flatMap(this::vetRequestResponse)
//                    .flatMap(this::ownerRequestResponse)
//                    .map(EntityDtoUtil::toBillEntityRC)
                    .map(EntityDtoUtil::toBillEntity)
                    .doOnNext(e -> e.setBillId(EntityDtoUtil.generateUUIDString()))
                    .flatMap(billRepository::insert)
                    .map(EntityDtoUtil::toBillResponseDto);
    }


    @Override
    public Mono<BillResponseDTO> updateBill(String billId, Mono<BillRequestDTO> billRequestDTO) {
        return billRequestDTO
                .flatMap(r -> billRepository.findByBillId(billId)
                        .flatMap(existingBill -> {
                            existingBill.setCustomerId(r.getCustomerId());
                            existingBill.setVisitType(r.getVisitType());
                            existingBill.setVetId(r.getVetId());
                            existingBill.setDate(r.getDate());
                            existingBill.setBillStatus(r.getBillStatus());
                            existingBill.setAmount(r.getAmount());
                            existingBill.setDueDate(r.getDueDate());

                            return billRepository.save(existingBill);
                        })
                        .map(EntityDtoUtil::toBillResponseDto)
                );

    }

    @Override
    public Mono<Void> deleteAllBills() {
        return billRepository.deleteAll();
    }


    @Override
    public Mono<Void> deleteBill(String billId) {
        return billRepository.findByBillId(billId)
                .flatMap(bill -> {
                    if (bill.getBillStatus() == BillStatus.UNPAID || bill.getBillStatus() == BillStatus.OVERDUE) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot delete a bill that is unpaid or overdue."));
                    }
                    return billRepository.deleteBillByBillId(billId);
                });
    }


    @Override
    public Flux<Void> deleteBillsByVetId(String vetId) {
        return billRepository.deleteBillsByVetId(vetId);
    }

    @Override
    public Flux<BillResponseDTO> getBillsByCustomerId(String customerId) {
/**/
        return billRepository.findByCustomerId(customerId).map(EntityDtoUtil::toBillResponseDto);
    }



    @Override
    public Flux<BillResponseDTO> getBillsByVetId(String vetId) {
        return billRepository.findByVetId(vetId).map(EntityDtoUtil::toBillResponseDto);
    }


    @Override
    public Flux<Void> deleteBillsByCustomerId(String customerId){
        return billRepository.deleteBillsByCustomerId(customerId);

    }
/*
    private long timeRemaining(BillResponseDTO bill){
        if (bill.getDueDate().isBefore(LocalDate.now())) {
            return 0;
        }

        return Duration.between(LocalDate.now().atStartOfDay(), bill.getDueDate().atStartOfDay()).toDays();
    }

 */



//    private Mono<RequestContextAdd> vetRequestResponse(RequestContextAdd rc) {
//        return
//                this.vetClient.getVetByVetId(rc.getBillRequestDTO().getVetId())
//                        .doOnNext(rc::setVetDTO)
//                        .thenReturn(rc);
//    }
//    private Mono<RequestContextAdd> ownerRequestResponse(RequestContextAdd rc) {
//        return
//                this.ownerClient.getOwnerByOwnerId(rc.getBillRequestDTO().getCustomerId())
//                        .doOnNext(rc::setOwnerResponseDTO)
//                        .thenReturn(rc);
//    }


    // Fetch a specific bill for a customer
    @Override
    public Mono<BillResponseDTO> getBillByCustomerIdAndBillId(String customerId, String billId) {
        return billRepository.findByBillId(billId)
                .filter(bill -> bill.getCustomerId().equals(customerId))
                .map(EntityDtoUtil::toBillResponseDto);
    }

    // Fetch filtered bills by status for a customer
    @Override
    public Flux<BillResponseDTO> getBillsByCustomerIdAndStatus(String customerId, BillStatus status) {
        return billRepository.findByCustomerIdAndBillStatus(customerId, status)
                .map(EntityDtoUtil::toBillResponseDto);
    }

    @Override
    public Mono<byte[]> generateBillPdf(String customerId, String billId) {
        return billRepository.findByBillId(billId)
                .filter(bill -> bill.getCustomerId().equals(customerId))
                .switchIfEmpty(Mono.error(new RuntimeException("Bill not found for given customer")))
                .map(EntityDtoUtil::toBillResponseDto)
                .flatMap(bill -> {
                    try {
                        byte[] pdfBytes = PdfGenerator.generateBillPdf(bill);
                        return Mono.just(pdfBytes);
                    } catch (DocumentException e) {
                        return Mono.error(new RuntimeException("Error generating PDF", e));
                    }
                });
    }

    @Override
    public Flux<BillResponseDTO> getBillsByMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth().plusDays(1);

        return billRepository.findByDateBetween(start, end)
                .map(EntityDtoUtil::toBillResponseDto)
                .switchIfEmpty(Flux.empty());
    }

    public Mono<Double> calculateCurrentBalance(String customerId) {
        return billRepository.findByCustomerIdAndBillStatus(customerId, BillStatus.UNPAID)
                .concatWith(billRepository.findByCustomerIdAndBillStatus(customerId, BillStatus.OVERDUE))
                .map(Bill::getAmount)
                .reduce(0.0, Double::sum)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found")));
    }           

    @Override
    public Mono<Bill> processPayment(String customerId, String billId, PaymentRequestDTO paymentRequestDTO) throws InvalidPaymentException {
        // Basic card validation outside of reactive pipeline
        if (paymentRequestDTO.getCardNumber().length() != 16 ||
                paymentRequestDTO.getCvv().length() != 3 ||
                paymentRequestDTO.getExpirationDate().length() != 5) {
            return Mono.error(new InvalidPaymentException("Invalid payment details"));
        }

        // Continue with reactive processing if validation is successful
        return billRepository.findByCustomerIdAndBillId(customerId, billId)
                .switchIfEmpty(Mono.error(new NotFoundException("Bill not found")))
                .flatMap(bill -> {
                    bill.setBillStatus(BillStatus.PAID);
                    return billRepository.save(bill);
                });
    }

}