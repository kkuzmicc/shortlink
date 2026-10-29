package com.shortlink.links;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import org.hibernate.exception.ConstraintViolationException;
import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;

@Path("/api/links")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LinkResource {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern ALIAS = Pattern.compile("[A-Za-z0-9_-]{4,32}");
    private static final int MAX_CODE_ATTEMPTS = 5;

    @Inject
    LinkRepository repository;

    @POST
    public Response create(CreateLinkRequest request) {
        String error = validate(request);

        if (error != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiError(error)).build();
        }

        String target = request.targetUrl().trim();
        String title = request.title() == null || request.title().isBlank() ? null : request.title().trim();

        boolean customAlias = request.customAlias() != null && !request.customAlias().isBlank();
        int attempts = customAlias ? 1 : MAX_CODE_ATTEMPTS;

        for (int attempt = 0; attempt < attempts; attempt++) {
            String code = customAlias ? request.customAlias().trim() : randomCode();

            try {
                LinkRepository.Link link = repository.insert(code, target, title);

                return Response.created(URI.create("/api/links/" + code)).entity(link).build();
            } catch (RuntimeException e) {
                if (!isCodeConflict(e)) {
                    throw e;
                }

                if (customAlias) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(new ApiError("Custom alias is already in use"))
                            .build();
                }
            }
        }
        throw new IllegalStateException("Could not allocate a unique short code");
    }

    @GET
    public Response list(@QueryParam("limit") @DefaultValue("20") int limit,
                         @QueryParam("offset") @DefaultValue("0") int offset) {
        if (limit < 1 || limit > 100 || offset < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("limit must be 1–100 and offset must be nonnegative"))
                    .build();
        }
        List<LinkRepository.Link> links = repository.getList(limit, offset);
        return Response.ok(links).build();
    }

    private String validate(CreateLinkRequest request) {
        if (request == null || request.targetUrl() == null || request.targetUrl().isBlank()) {
            return "targetUrl is required";
        }

        String target = request.targetUrl().trim();
        if (target.length() > 2048) {
            return "targetUrl must be at most 2048 characters";
        }

        try {
            URI uri = URI.create(target);

            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null) {
                return "targetUrl must be an absolute HTTP or HTTPS URL without credentials";
            }
        } catch (IllegalArgumentException e) {
            return "targetUrl must be a valid URL";
        }

        if (request.title() != null && request.title().trim().length() > 200) {
            return "title must be at most 200 characters";
        }

        if (request.customAlias() != null && !request.customAlias().isBlank()
                && !ALIAS.matcher(request.customAlias().trim()).matches()) {
            return "customAlias must be 4–32 letters, numbers, hyphens or underscores";
        }

        return null;
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(7);

        for (int i = 0; i < 7; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        return code.toString();
    }

    private boolean isCodeConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && "links_code_key".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    public record CreateLinkRequest(String targetUrl, String title, String customAlias) {}
    public record ApiError(String error) {}
}
