package org.migue.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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

    public List<PostResponse> getPostsWithDetails() {
        LOG.debug("Iniciando obtención de posts con detalles");
        List<PostDto> posts;
        try {
            posts = postClient.getPosts();
        } catch (Exception ex) {
            LOG.error("Fallo en servicio externo de posts", ex);
            throw new ExternalServiceException("Error al obtener posts", ex);
        }

        if (posts == null || posts.isEmpty()) {
            LOG.warn("No se encontraron posts");
            throw new ResourceNotFoundException("No hay posts disponibles");
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
                .collect(Collectors.toList());

        LOG.info("Posts procesados correctamente: {}", responses.size());
        return responses;
    }

    private Map<Long, List<CommentDto>> prefetchComments(List<PostDto> posts) {
        List<CompletableFuture<Map.Entry<Long, List<CommentDto>>>> commentFutures = posts.stream()
                .filter(post -> post != null && post.id != null)
                .map(post -> CompletableFuture.supplyAsync(
                        () -> Map.entry(post.id, fetchCommentsSafe(post.id)), executor
                ))
                .collect(Collectors.toList());

        return commentFutures.stream()
                .map(CompletableFuture::join)
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
                        throw new RuntimeException(ex);
                    }
                }, executor)
        );
    }

    private void populateAuthorInfo(PostResponse response, UserDto user) {
        if (user != null) {
            response.authorName = Objects.toString(user.name, "desconocido");
            response.authorEmail = Objects.toString(user.email, "desconocido");
        } else {
            setDefaultAuthor(response);
        }
    }

    private void setDefaultAuthor(PostResponse response) {
        response.authorName = "desconocido";
        response.authorEmail = "desconocido";
    }

    public Response deletePost(Long postId) {
        LOG.debug("Solicitud de borrado de postId {}", postId);
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("postId inválido");
        }

        try {
            Response response = postClient.deletePost(postId);
            if (response != null && (response.getStatus() == 200 || response.getStatus() == 204)) {
                LOG.info("Post {} eliminado correctamente", postId);
                return Response.noContent().build();
            }
            if (response != null && response.getStatus() == 404) {
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
