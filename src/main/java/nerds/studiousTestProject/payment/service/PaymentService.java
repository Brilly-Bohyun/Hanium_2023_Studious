package nerds.studiousTestProject.payment.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nerds.studiousTestProject.exception.PaymentNotFoundException;
import nerds.studiousTestProject.payment.dto.RequestToToss;
import nerds.studiousTestProject.payment.dto.cancel.CancelRequest;
import nerds.studiousTestProject.payment.dto.cancel.CancelResponse;
import nerds.studiousTestProject.payment.dto.confirm.*;
import nerds.studiousTestProject.payment.dto.request.PaymentRequest;
import nerds.studiousTestProject.payment.dto.request.PaymentResponse;
import nerds.studiousTestProject.payment.entity.Payment;
import nerds.studiousTestProject.payment.repository.PaymentRepository;
import nerds.studiousTestProject.reservationRecord.entity.ReservationRecord;
import nerds.studiousTestProject.reservationRecord.service.ReservationRecordService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static nerds.studiousTestProject.payment.PaymentConstant.*;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final WebClient webClient;
    private final ReservationRecordService reservationRecordService;
    private final PaymentRepository paymentRepository;
    private static final String CONFIRM_URI = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String CANCEL_URI = "https://api.tosspayments.com/v1/payments/%s/cancel";

    public PaymentResponse createPaymentResponse(PaymentRequest paymentRequest, String orderId) {
        return PaymentResponse.builder()
                .amount(paymentRequest.getReservation().getPrice())
                .orderId(orderId)
                .orderName(paymentRequest.getUser().getName())
                .successUrl("http://localhost:8080/studious/payments/success")
                .failUrl("http://localhost:8080/studious/payments/fail")
                .build();
    }

    @Transactional
    public ConfirmSuccessResponse confirmPayToToss(String orderId, String paymentKey, Integer amount) {
        ConfirmSuccessRequest request = ConfirmSuccessRequest.of(orderId,amount,paymentKey);
        PaymentResponseFromToss responseFromToss = requestToToss(request, CONFIRM_URI);
        Payment payment = Payment.builder()
                .completeTime(responseFromToss.getRequestedAt())
                .type(responseFromToss.getType())
                .orderId(responseFromToss.getOrderId())
                .paymentKey(responseFromToss.getPaymentKey())
                .build();
        reservationRecordService.findByOrderId(orderId).completePay(payment);//결제 완료로 상태 변경
        return createPaymentConfirmResponse(responseFromToss);
    }

    @NonNull
    private PaymentResponseFromToss requestToToss(RequestToToss request, String requestURI) {
        String secreteKey = Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes());
        PaymentResponseFromToss responseFromToss = webClient.method(HttpMethod.POST)
                .uri(requestURI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + secreteKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponseFromToss.class)
                .block();
        return responseFromToss;
    }

    public ConfirmSuccessResponse createPaymentConfirmResponse(PaymentResponseFromToss responseFromToss){
        ReservationRecord reservationRecord = reservationRecordService.findByOrderId(responseFromToss.getOrderId());
        return ConfirmSuccessResponse.builder()
                .reservationInfo(ReservationInfo.of(reservationRecord))
                .reserveUserInfo(ReserveUserInfo.of(reservationRecord))
                .build();
    }

    /*
     실패시 저장되었던 예약내역 삭제, 실패 정보 반환
     */
    @Transactional
    public ConfirmFailResponse confirmFail(String message, String orderId){
        reservationRecordService.deleteByOrderId(orderId);
        return ConfirmFailResponse.builder()
                .message(message)
                .statusCode(400)
                .build();
    }

    public List<CancelResponse> cancel(CancelRequest cancelRequest, Long reservationId){
        ReservationRecord reservationRecord = reservationRecordService.findById(reservationId);
        List<CancelResponse> cancelResponses = requestCancelToToss(cancelRequest, reservationRecord.getPayment().getPaymentKey());
        reservationRecordService.cancel(reservationId); //결제 취소 상태로 변경
        return cancelResponses;
    }
    @Transactional
    public List<CancelResponse> requestCancelToToss(CancelRequest cancelRequest, String paymentKey){
        PaymentResponseFromToss responseFromToss = requestToToss(cancelRequest, String.format(CANCEL_URI, paymentKey));
        List<CancelResponse> cancelResponses = new ArrayList<>();
        responseFromToss.getCancels().stream().forEach(cancel -> cancelResponses.add(CancelResponse.of(cancel)));
        deletePaymentByCancel(responseFromToss);
        return cancelResponses;
    }

    private void deletePaymentByCancel(PaymentResponseFromToss responseFromToss) {
        Payment payment = paymentRepository.findByPaymentKeyAndOrderId(
                        responseFromToss.getPaymentKey(),
                        responseFromToss.getOrderId())
                .orElseThrow(PaymentNotFoundException::new);
        paymentRepository.delete(payment);
    }

}
