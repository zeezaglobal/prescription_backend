package com.zeezaglobal.prescription.Service;

import com.zeezaglobal.prescription.DTO.ArticleRequestDTO;
import com.zeezaglobal.prescription.Entities.Article;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Repository.ArticleRepository;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);

    private final ArticleRepository articleRepository;
    private final DoctorRepository doctorRepository;

    public Article createArticle(ArticleRequestDTO dto, String doctorUsername) {
        Doctor doctor = doctorRepository.findByUsername(doctorUsername)
                .orElseThrow(() -> new RuntimeException("Doctor not found with username: " + doctorUsername));

        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : Article.ArticleStatus.PUBLISHED);
        article.setDoctor(doctor);

        logger.info("Creating article '{}' by doctor: {}", dto.getTitle(), doctorUsername);
        return articleRepository.save(article);
    }

    public Article updateArticle(Long id, ArticleRequestDTO dto, String doctorUsername) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));

        if (!article.getDoctor().getUsername().equals(doctorUsername)) {
            throw new RuntimeException("You are not authorized to update this article");
        }

        if (dto.getTitle() != null) article.setTitle(dto.getTitle());
        if (dto.getSummary() != null) article.setSummary(dto.getSummary());
        if (dto.getContent() != null) article.setContent(dto.getContent());
        if (dto.getStatus() != null) article.setStatus(dto.getStatus());

        return articleRepository.save(article);
    }

    public void deleteArticle(Long id, String doctorUsername) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));

        if (!article.getDoctor().getUsername().equals(doctorUsername)) {
            throw new RuntimeException("You are not authorized to delete this article");
        }

        articleRepository.delete(article);
    }

    public Article getArticleById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
    }

    public Page<Article> getPublishedArticles(Pageable pageable) {
        return articleRepository.findByStatus(Article.ArticleStatus.PUBLISHED, pageable);
    }

    public Page<Article> getArticlesByAuthenticatedDoctor(String doctorUsername, Pageable pageable) {
        Doctor doctor = doctorRepository.findByUsername(doctorUsername)
                .orElseThrow(() -> new RuntimeException("Doctor not found with username: " + doctorUsername));
        return articleRepository.findByDoctor(doctor, pageable);
    }

    public Page<Article> searchPublishedArticles(String searchTerm, Pageable pageable) {
        return articleRepository.searchPublished(searchTerm, pageable);
    }
}
