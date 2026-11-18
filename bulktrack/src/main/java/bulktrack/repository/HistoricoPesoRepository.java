package bulktrack.repository;

import bulktrack.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoPesoRepository extends JpaRepository<HistoricoPeso, Long> {
    List<HistoricoPeso> findByPerfilOrderByDataDescIdDesc(Perfil perfil);
    Optional<HistoricoPeso> findFirstByPerfilOrderByDataDescIdDesc(Perfil perfil);
}
