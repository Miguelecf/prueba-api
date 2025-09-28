package org.migue.service;

        import jakarta.enterprise.context.ApplicationScoped;
        import jakarta.ws.rs.NotFoundException;
        import jakarta.ws.rs.core.Response;
        import org.eclipse.microprofile.rest.client.inject.RestClient;
        import org.migue.client.CommentClient;
        import org.migue.client.UserClient;
        import org.migue.client.PostClient;
        import org.migue.dto.CommentDto;
        import org.migue.dto.PostDto;
        import org.migue.dto.PostResponse;
        import org.migue.exception.*;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.List;
        import java.util.Objects;

        @ApplicationScoped
        public class PostService {

            private static final Logger LOG = LoggerFactory.getLogger(PostService.class);

            @RestClient
            private PostClient postClient;

            @RestClient
            private CommentClient commentClient;

            @RestClient
            private UserClient userClient;

            public List<PostResponse> getPostsWithDetails() {
                LOG.debug("Iniciando obtención de posts con detalles");
                List<PostDto> posts;
                try {
                    posts = postClient.getPosts();

                } catch (ExternalServiceException ex) {
                    LOG.error("Servicio de posts no disponible", ex);
                    return Collections.emptyList();

                } catch (Exception ex) {
                    LOG.error("Error inesperado obteniendo posts desde PostClient", ex);
                    return Collections.emptyList();
                }

                if (posts == null || posts.isEmpty()) {
                    LOG.info("No se encontraron posts");
                    return Collections.emptyList();
                }

                List<PostResponse> postResponses = new ArrayList<>();
                for (PostDto post : posts) {
                    if (post == null || post.id == null) {
                        LOG.warn("Post nulo o sin id, se omite");
                        continue;
                    }

                    PostResponse postResponse = new PostResponse();
                    postResponse.id = post.id;
                    postResponse.title = post.title != null ? post.title : "";
                    postResponse.body = post.body != null ? post.body : "";

                    postResponse.comments = fetchCommentsSafe(post.id);
                    populateAuthorSafe(postResponse, post.userId);

                    postResponses.add(postResponse);
                }

                LOG.debug("Finalizada construcción de {} respuestas de posts", postResponses.size());
                return postResponses;
            }

            private List<CommentDto> fetchCommentsSafe(Long postId) {
                try {
                    List<CommentDto> comments = commentClient.getComments(postId);
                    return comments != null ? comments : Collections.emptyList();

                } catch (NotFoundException ex) {
                    LOG.warn("Comentarios no encontrados para postId {}: {}", postId, ex.getMessage());
                    return Collections.emptyList();

                } catch (ExternalServiceException ex) {
                    LOG.warn("Servicio de comentarios no disponible para postId {}: {}", postId, ex.getMessage());
                    return Collections.emptyList();

                } catch (Exception ex) {
                    LOG.warn("Error inesperado obteniendo comentarios para postId {}: {}", postId, ex.getMessage());
                    return Collections.emptyList();
                }
            }

            private void populateAuthorSafe(PostResponse postResponse, Long userId) {
                if (userId == null) {
                    postResponse.authorName = "desconocido";
                    postResponse.authorEmail = "desconocido";
                    return;
                }

                try {
                    var user = userClient.getUser(userId);

                    if (user != null) {
                        postResponse.authorName = Objects.toString(user.name, "desconocido");
                        postResponse.authorEmail = Objects.toString(user.email, "desconocido");
                    } else {
                        LOG.warn("Usuario no encontrado para userId {}", userId);
                        postResponse.authorName = "desconocido";
                        postResponse.authorEmail = "desconocido";
                    }
                } catch (NotFoundException ex) {
                    LOG.warn("Usuario no encontrado para userId {}: {}", userId, ex.getMessage());
                    postResponse.authorName = "desconocido";
                    postResponse.authorEmail = "desconocido";
                } catch (ExternalServiceException ex) {
                    LOG.warn("Servicio de usuarios no disponible para userId {}: {}", userId, ex.getMessage());
                    postResponse.authorName = "desconocido";
                    postResponse.authorEmail = "desconocido";
                } catch (Exception ex) {
                    LOG.warn("Error inesperado obteniendo usuario para userId {}: {}", userId, ex.getMessage());
                    postResponse.authorName = "desconocido";
                    postResponse.authorEmail = "desconocido";
                }
            }

            public Response deletePost(Long postId) {
                LOG.debug("Solicitud de borrado de postId {}", postId);
                if (postId == null || postId <= 0) {
                    LOG.warn("postId inválido: {}", postId);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("postId inválido")
                            .build();
                }

                try {
                    Response response = postClient.deletePost(postId);

                    if (response == null) {
                        LOG.error("PostClient devolvió respuesta nula al eliminar postId {}", postId);
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }

                    LOG.info("Eliminación de postId {} finalizada con estado {}", postId, response.getStatus());
                    return response;

                } catch (NotFoundException ex) {
                    LOG.info("Post no encontrado al intentar eliminar postId {}: {}", postId, ex.getMessage());
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("Post no encontrado")
                            .build();
                } catch (ExternalServiceException ex) {
                    LOG.error("Servicio de posts no disponible al eliminar postId {}: {}", postId, ex.getMessage());
                    return Response.status(Response.Status.BAD_GATEWAY)
                            .entity("Servicio externo no disponible")
                            .build();
                } catch (Exception ex) {
                    LOG.error("Error al eliminar postId {}", postId, ex);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error interno al eliminar el post")
                            .build();
                }
            }
        }