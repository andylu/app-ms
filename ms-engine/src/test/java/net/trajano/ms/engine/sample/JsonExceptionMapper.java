package net.trajano.ms.engine.sample;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonExceptionMapper implements
    ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(final Throwable exception) {

        exception.printStackTrace();
        return Response.ok(new ErrorResponse(exception)).status(Status.INTERNAL_SERVER_ERROR).build();
    }

}
