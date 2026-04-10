package se.exempel.sds.repository;

import se.exempel.sds.domain.Arende;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import se.exempel.sds.domain.ArendeStatus;

@Repository
public interface ArendeRepository extends JpaRepository<Arende, UUID> {

    Optional<Arende> findByIdAndOrganisationId(UUID id, String organisationId);

    List<Arende> findAllByOrganisationId(String organisationId);

    List<Arende> findAllByOrganisationIdAndStatus(String organisationId, ArendeStatus status);
}
