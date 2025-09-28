package org.migue.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.migue.dto.CommentDto;

import java.util.List;

@Path("/posts")
@RegisterRestClient(configKey = "comment-api")
public interface CommentClient {

    @GET
    @Path("/{id}/comments")
    @Produces(MediaType.APPLICATION_JSON)
    List<CommentDto> getComments(@PathParam("id") Long postId);
}
