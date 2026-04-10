package se.exempel.sds.repository;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.Arende;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
class OrganisationRepositoryTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DokumentRepository dokumentRepository;

    @Autowired
    private ArendeRepository arendeRepository;

    // Dokument åtskilda mellan organisationer

    @Test
    void findByIdAndOrganisationId_shouldReturnDokument_forCorrectOrganisation() {
        Dokument doc = saved(dokument("organisation-A", Classification.PUBLIC));

        Optional<Dokument> result =
                dokumentRepository.findByIdAndOrganisationId(doc.getId(), "organisation-A");

        assertThat(result).isPresent();
    }

    @Test
    void findByIdAndOrganisationId_shouldReturnEmpty_forWrongOrganisation() {
        Dokument doc = saved(dokument("organisation-A", Classification.PUBLIC));

        Optional<Dokument> result =
                dokumentRepository.findByIdAndOrganisationId(doc.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByOrganisationId_shouldOnlyReturnOwnOrganisationDokuments() {
        saved(dokument("organisation-A", Classification.PUBLIC));
        saved(dokument("organisation-A", Classification.INTERNAL));
        saved(dokument("organisation-B", Classification.PUBLIC));

        List<Dokument> result = dokumentRepository.findAllByOrganisationId("organisation-A");

        assertThat(result).hasSize(2)
                .allMatch(d -> d.getOrganisationId().equals("organisation-A"));
    }

    @Test
    void findAllByOrganisationId_shouldReturnEmpty_whenOrganisationHasNoDokuments() {
        saved(dokument("organisation-A", Classification.PUBLIC));

        List<Dokument> result = dokumentRepository.findAllByOrganisationId("organisation-C");

        assertThat(result).isEmpty();
    }

    // Arende per organisation

    @Test
    void findArendeByIdAndOrganisationId_shouldReturnEmpty_forWrongOrganisation() {
        Arende arende = saved(arende("organisation-A"));

        Optional<Arende> result =
                arendeRepository.findByIdAndOrganisationId(arende.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllArendenByOrganisationId_shouldOnlyReturnOwnOrganisationArenden() {
        saved(arende("organisation-A"));
        saved(arende("organisation-A"));
        saved(arende("organisation-B"));

        List<Arende> result = arendeRepository.findAllByOrganisationId("organisation-A");

        assertThat(result).hasSize(2)
                .allMatch(c -> c.getOrganisationId().equals("organisation-A"));
    }

    // Dokument+Arende koppling

    @Test
    void findAllByLinkedArende_shouldReturnEmpty_forWrongOrganisation() {
        Arende arende = saved(arende("organisation-A"));
        Dokument doc = dokument("organisation-A", Classification.PUBLIC);
        doc.setLinkedArende(arende);
        saved(doc);

        List<Dokument> result =
                dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(arende.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByLinkedArende_shouldReturnDokuments_forCorrectOrganisation() {
        Arende arende = saved(arende("organisation-A"));
        Dokument doc = dokument("organisation-A", Classification.PUBLIC);
        doc.setLinkedArende(arende);
        saved(doc);

        List<Dokument> result =
                dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(arende.getId(), "organisation-A");

        assertThat(result).hasSize(1);
    }

    // Helpers

    private <T> T saved(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    private static Dokument dokument(String organisationId, Classification classification) {
        return Dokument.builder()
                .organisationId(organisationId)
                .title("Test dokument")
                .classification(classification)
                .createdBy("test-user")
                .build();
    }

    private static Arende arende(String organisationId) {
        return Arende.builder()
                .organisationId(organisationId)
                .title("Test arende")
                .status(ArendeStatus.OPEN)
                .createdBy("test-user")
                .build();
    }
}
