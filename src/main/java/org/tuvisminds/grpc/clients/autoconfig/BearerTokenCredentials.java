package org.tuvisminds.grpc.clients.autoconfig;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class BearerTokenCredentials extends CallCredentials {
    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    public static String BEARER_TYPE = "Bearer";
    String token;

    public BearerTokenCredentials(String bearerType, String token) {
        BEARER_TYPE = bearerType;
        this.token = token;
    }
    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(AUTHORIZATION_METADATA_KEY, String.format("%s %s", BEARER_TYPE, token));
                applier.apply(headers);
            } catch (Throwable e) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {

    }
}
