package org.migue.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.migue.client.CommentClient;
import org.migue.client.PostClient;
import org.migue.client.UserClient;
import org.migue.dto.CommentDto;
import org.migue.dto.PostDto;
import org.migue.dto.PostResponse;
import org.migue.dto.UserDto;
import org.migue.exception.ExternalServiceException;
import org.migue.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import static org.migue.utils.PostServiceConstants.*;

@ApplicationScoped
public class PostService {

    private static final Logger LOG = LoggerFactory.getLogger(PostService.class);


    @Inject
    @RestClient
    PostClient postClient;

    @Inject
    @RestClient
    CommentClient commentClient;

    @Inject
    @RestClient
    UserClient userClient;

    @Inject
    Executor executor;

    // Configuraciones externalizadas
    @ConfigProperty(name = "app.external.timeout.ms", defaultValue = "5000")
    long externalTimeoutMs;

    @ConfigProperty(name = "app.external.max-posts", defaultValue = "1000")
    int maxPostsLimit;

    public List<PostResponse> getPostsWithDetails() {
        LOG.debug("Iniciando obtención de posts con detalles");

        long startTime = System.currentTimeMillis();

        try {
            List<PostDto> posts = fetchPostsWithTimeout();

            if (posts == null || posts.isEmpty()) {
                LOG.warn("No se encontraron posts");
                throw new ResourceNotFoundException("No hay posts disponibles");
            }

            // Limitar el número de posts procesados
            if (posts.size() > maxPostsLimit) {
                LOG.warn("Limité el número de posts de {} a {}", posts.size(), maxPostsLimit);
                posts = posts.subList(0, maxPostsLimit);
            }

            Map<Long, List<CommentDto>> commentsMap = prefetchComments(posts);
            Map<Long, CompletableFuture<UserDto>> userCache = new ConcurrentHashMap<>();

            List<CompletableFuture<PostResponse>> futures = posts.stream()
                    .filter(post -> post != null && post.id != null)
                    .map(post -> createPostResponseFuture(
                            post,
                            commentsMap.getOrDefault(post.id, Collections.emptyList()),
                            userCache
                    ))
                    .collect(Collectors.toList());

            List<PostResponse> responses = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            long processingTime = System.currentTimeMillis() - startTime;
            LOG.info("Posts procesados correctamente: {} en {} ms", responses.size(), processingTime);

            return responses;

        } catch (ResourceNotFoundException | ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Error inesperado al obtener posts con detalles", ex);
            throw new ExternalServiceException("Error inesperado al procesar los posts", ex);
        }
    }

    private List<PostDto> fetchPostsWithTimeout() {
        try {
            return CompletableFuture.supplyAsync(() -> postClient.getPosts(), executor)
                    .orTimeout(externalTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .join();
        } catch (Exception ex) {
            LOG.error("Fallo en servicio externo de posts", ex);
            throw new ExternalServiceException("Error al obtener posts", ex);
        }
    }

    private Map<Long, List<CommentDto>> prefetchComments(List<PostDto> posts) {
        List<CompletableFuture<Map.Entry<Long, List<CommentDto>>>> commentFutures = posts.stream()
                .filter(post -> post != null && post.id != null)
                .map(post -> CompletableFuture.supplyAsync(
                        () -> Map.entry(post.id, fetchCommentsSafe(post.id)), executor
                ).orTimeout(externalTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());

        return commentFutures.stream()
                .map(future -> {
                    try {
                        return future.join();
                    } catch (Exception ex) {
                        LOG.warn("Error al obtener comentarios, usando lista vacía", ex);
                        return Map.entry(-1L, Collections.<CommentDto>emptyList());
                    }
                })
                .filter(entry -> entry.getKey() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<CommentDto> fetchCommentsSafe(Long postId) {
        try {
            List<CommentDto> comments = commentClient.getComments(postId);
            return comments != null ? comments : Collections.emptyList();
        } catch (NotFoundException ex) {
            LOG.warn("Comentarios no encontrados para postId {}", postId);
            return Collections.emptyList();
        } catch (Exception ex) {
            LOG.error("Fallo al obtener comentarios para postId {}", postId, ex);
            return Collections.emptyList();
        }
    }

    private CompletableFuture<PostResponse> createPostResponseFuture(
            PostDto post,
            List<CommentDto> comments,
            Map<Long, CompletableFuture<UserDto>> userCache) {

        PostResponse response = new PostResponse();
        response.id = post.id;
        response.title = Objects.toString(post.title, "");
        response.body = Objects.toString(post.body, "");
        response.comments = comments;

        return getOrCreateUserFuture(post.userId, userCache)
                .thenApply(user -> {
                    populateAuthorInfo(response, user);
                    return response;
                })
                .exceptionally(ex -> {
                    LOG.error("Error procesando usuario para post {}. Asignando autor por defecto.", post.id, ex);
                    setDefaultAuthor(response);
                    return response;
                });
    }

    private CompletableFuture<UserDto> getOrCreateUserFuture(Long userId, Map<Long, CompletableFuture<UserDto>> userCache) {
        if (userId == null) {
            return CompletableFuture.completedFuture(null);
        }

        return userCache.computeIfAbsent(userId, id ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return userClient.getUser(id);
                    } catch (Exception ex) {
                        LOG.error("Error obteniendo usuario {}", id, ex);
                        throw new RuntimeException("Error obteniendo usuario " + id, ex);
                    }
                }, executor).orTimeout(externalTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        );
    }

    private void populateAuthorInfo(PostResponse response, UserDto user) {
        if (user != null) {
            response.authorName = Objects.toString(user.name, DEFAULT_AUTHOR_NAME);
            response.authorEmail = Objects.toString(user.email, DEFAULT_AUTHOR_EMAIL);
        } else {
            setDefaultAuthor(response);
        }
    }

    private void setDefaultAuthor(PostResponse response) {
        response.authorName = DEFAULT_AUTHOR_NAME;
        response.authorEmail = DEFAULT_AUTHOR_EMAIL;
    }

    public Response deletePost(Long postId) {
        LOG.debug("Solicitud de borrado de postId {}", postId);
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("postId inválido");
        }

        try {
            Response response = CompletableFuture.supplyAsync(
                    () -> postClient.deletePost(postId), executor
            ).orTimeout(externalTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS).join();

            if (response != null && (response.getStatus() == SUCCESS_DELETE_STATUS_200 ||
                    response.getStatus() == SUCCESS_DELETE_STATUS_204)) {
                LOG.info("Post {} eliminado correctamente", postId);
                return Response.noContent().build();
            }

            if (response != null && response.getStatus() == NOT_FOUND_STATUS) {
                throw new ResourceNotFoundException("Post no encontrado con id " + postId);
            }

            LOG.error("Respuesta inesperada al eliminar postId {}: {}", postId,
                    response != null ? response.getStatus() : "null");
            throw new ExternalServiceException("Error eliminando post en servicio externo");

        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Fallo eliminando post {}", postId, ex);
            throw new ExternalServiceException("Error eliminando post", ex);
        }
    }
}