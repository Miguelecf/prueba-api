package org.migue.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private static final Logger logger = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    public Response toResponse(NotFoundException exception) {
        logger.warn("NotFoundException atrapada: {}", exception.getMessage(), exception);
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(404, exception.getMessage(), LocalDateTime.now().toString()))
                .build();
    }

}