package nerds.studiousTestProject.studycafe.dto.modify.response;

import lombok.Builder;
import lombok.Data;
import nerds.studiousTestProject.studycafe.entity.Address;

@Builder
@Data
public class AddressInfoResponse {
    private String basic;   // 기본 주소
    private String detail;  // 상세 주소
    private String zipcode; // 우편 번호

    public static AddressInfoResponse from(Address address) {
        return AddressInfoResponse.builder()
                .basic(address.getAddressBasic())
                .detail(address.getAddressDetail())
                .zipcode(address.getAddressZipcode())
                .build();
    }
}