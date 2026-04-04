package com.finresearch.repository;

import com.finresearch.domain.PaperVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperVersionRepository extends JpaRepository<PaperVersion, Long> {
    List<PaperVersion> findByPaperIdOrderByVersionNoDesc(Long paperId);

    Optional<PaperVersion> findByPaperIdAndVersionNo(Long paperId, int versionNo);
}
