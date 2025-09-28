package org.migue.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.migue.dto.PostResponse;
import org.migue.exception.ExternalServiceException;
import org.migue.service.PostService;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/posts")
public class PostResource {

    private static final Logger LOG = Logger.getLogger(PostResource.class);

    @Inject
    PostService postService;

@GET
@Produces(MediaType.APPLICATION_JSON)
public List<PostResponse> getAllPosts(
        @QueryParam("authorId") Long authorId,
        @QueryParam("search") String search,
        @DefaultValue("100") @QueryParam("limit") int limit,
        @DefaultValue("0") @QueryParam("offset") int offset) {

    LOG.infof("GET /posts llamada con authorId=%s search=%s limit=%d offset=%d",
            authorId, search, limit, offset);

    try {
        // Validaciones básicas de entrada
        if (authorId != null && authorId <= 0) {
            LOG.warn("authorId inválido");
            throw new jakarta.ws.rs.BadRequestException("authorId inválido");
        }
        if (limit <= 0 || limit > 500) {
            LOG.warn("limit fuera de rango");
            throw new jakarta.ws.rs.BadRequestException("limit debe estar entre 1 y 500");
        }
        if (offset < 0) {
            LOG.warn("offset inválido");
            throw new jakarta.ws.rs.BadRequestException("offset no puede ser negativo");
        }

        // Limpieza simple del paramámetro de búsqueda
        if (search != null) {
            search = search.trim();
            if (search.length() > 200) {
                LOG.warn("search demasiado largo");
                throw new jakarta.ws.rs.BadRequestException("search demasiado largo");
            }
            // Eliminar caracteres potencialmente peligrosos
            search = search.replaceAll("[<>\\p{Cntrl}]", "");
            if (search.isEmpty()) {
                search = null;
            }
        }

        List<PostResponse> posts = postService.getPostsWithDetails();

        if (posts == null || posts.isEmpty()) {
            LOG.warn("No se encontraron posts");
            throw new jakarta.ws.rs.NotFoundException("No se encontraron posts");
        }
        LOG.infof("GET /posts retornó %d resultados", posts.size());
        return posts;


    } catch (jakarta.ws.rs.WebApplicationException wae) {
        throw wae;
    } catch (Exception e) {
        LOG.error("Error al obtener los posts con detalles: " + e.getMessage(), e);
        throw new ExternalServiceException("Error al obtener los posts desde el servicio externo", e);
    }
}

    @DELETE
    @Path("/{id}")
    public Response deletePost(@PathParam("id") Long postId) {
        LOG.infof("DELETE /posts llamada con id=%s", postId);
        try {
            // Validaciones básicas de entrada
            if (postId == null) {
                LOG.warn("postId nulo");
                throw new jakarta.ws.rs.BadRequestException("postId es requerido");
            }
            if (postId <= 0) {
                LOG.warn("postId inválido");
                throw new jakarta.ws.rs.BadRequestException("postId inválido");
            }

            Response response = postService.deletePost(postId);

            if (response == null) {
                LOG.error("Respuesta nula del servicio al eliminar post");
                throw new ExternalServiceException("Respuesta nula del servicio externo");
            }

            int status = response.getStatus();
            if (status == 200 || status == 204) {
                LOG.infof("POST %d eliminado, status=%d", postId, status);
                return Response.noContent().build();
            }
            if (status == 404) {
                LOG.warnf("Post %d no encontrado", postId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (status >= 400 && status < 500) {
                LOG.warnf("Error del cliente al eliminar post %d status=%d", postId, status);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            LOG.errorf("Error del servicio externo al eliminar post %d status=%d", postId, status);
            return Response.status(Response.Status.BAD_GATEWAY).build();

        } catch (jakarta.ws.rs.WebApplicationException wae) {
            throw wae;
        } catch (Exception e) {
            LOG.error("Error al eliminar el post: " + e.getMessage(), e);
            throw new ExternalServiceException("Error al eliminar el post desde el servicio externo", e);
        }
    }

}
