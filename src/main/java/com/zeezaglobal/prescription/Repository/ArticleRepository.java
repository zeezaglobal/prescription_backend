package com.zeezaglobal.prescription.Repository;

import com.zeezaglobal.prescription.Entities.Article;
import com.zeezaglobal.prescription.Entities.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Page<Article> findByStatus(Article.ArticleStatus status, Pageable pageable);

    Page<Article> findByDoctor(Doctor doctor, Pageable pageable);

    Page<Article> findByDoctorAndStatus(Doctor doctor, Article.ArticleStatus status, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
            "(LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Article> searchPublished(@Param("searchTerm") String searchTerm, Pageable pageable);
}
