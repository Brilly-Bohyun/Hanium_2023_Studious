package nerds.studiousTestProject.user.repository.member;

import nerds.studiousTestProject.user.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByEmailAndPassword(String email, String encode);
    Optional<Member> findByProviderId(Long providerId);
    boolean existsByProviderId(Long providerId);
    boolean existsByEmail(String email);
}