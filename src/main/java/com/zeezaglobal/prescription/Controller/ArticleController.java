package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.ArticleRequestDTO;
import com.zeezaglobal.prescription.Entities.Article;
import com.zeezaglobal.prescription.Service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * Create a new article (Doctors only)
     * POST /api/articles
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createArticle(@RequestBody ArticleRequestDTO dto, Authentication authentication) {
        try {
            Article article = articleService.createArticle(dto, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(article);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update an article (only the authoring doctor)
     * PUT /api/articles/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateArticle(
            @PathVariable Long id,
            @RequestBody ArticleRequestDTO dto,
            Authentication authentication) {
        try {
            Article article = articleService.updateArticle(id, dto, authentication.getName());
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete an article (only the authoring doctor)
     * DELETE /api/articles/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> deleteArticle(@PathVariable Long id, Authentication authentication) {
        try {
            articleService.deleteArticle(id, authentication.getName());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Article deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get a single article by id
     * GET /api/articles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getArticleById(@PathVariable Long id) {
        try {
            Article article = articleService.getArticleById(id);
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all published articles (paginated)
     * GET /api/articles?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<?> getPublishedArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Article> articles = articleService.getPublishedArticles(pageable);
            return ResponseEntity.ok(createPageResponse(articles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get the authenticated doctor's own articles (drafts and published)
     * GET /api/articles/doctor
     */
    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Article> articles = articleService.getArticlesByAuthenticatedDoctor(authentication.getName(), pageable);
            return ResponseEntity.ok(createPageResponse(articles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Search published articles by title or summary
     * GET /api/articles/search?query=diabetes
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Article> articles = articleService.searchPublishedArticles(query, pageable);
            return ResponseEntity.ok(createPageResponse(articles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }

    private Map<String, Object> createPageResponse(Page<Article> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalElements", page.getTotalElements());
        response.put("pageSize", page.getSize());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        return response;
    }
}
