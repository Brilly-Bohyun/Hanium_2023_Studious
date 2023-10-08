package nerds.studiousTestProject.reservation.controller;

import lombok.RequiredArgsConstructor;
import nerds.studiousTestProject.common.util.LoggedInMember;
import nerds.studiousTestProject.reservation.dto.admin.ShowAdminCancelResponse;
import nerds.studiousTestProject.reservation.service.ReservationRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
@RestController
public class AdminReservationRecordController {

    private final ReservationRecordService reservationRecordService;

    @GetMapping("/{reservationId}/cancel")
    public ShowAdminCancelResponse showCancel(@PathVariable Long reservationId) {
        return reservationRecordService.showAdminCancel(reservationId);
    }

}
