package org.tuvisminds.grpc.clients.autoconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class AutoConfigGrpcClient {
    AbstractStub abstractStub;

    public class Response {
        // Tells if GRPC call was successful - can be true or false
        public boolean callSuccess;
        // Status returned via GRPC call
        public int status;
        // Value will be set only in case exception - in short it is exception message
        public String description;
        // This response message from GRPC api - in case call was successful
        public JsonNode message;
    }

    public AutoConfigGrpcClient(String grpcServerName, int grpcServerPort, String grpcServiceName, String bearerType, String authToken) throws Exception {
        try {
            ManagedChannel managedChannel = NettyChannelBuilder.forAddress(grpcServerName, grpcServerPort).usePlaintext().build();
            BearerTokenCredentials bearerTokenCredentials = new BearerTokenCredentials(bearerType, authToken);
            Class<?> clazz = Class.forName(grpcServiceName);
            Method method = getByName(clazz.getMethods(), "newBlockingStub");
            Object stubObject = method.invoke(null, managedChannel);
            Class<?> stubClass = stubObject.getClass();
            abstractStub = ((AbstractStub) stubClass.cast(stubObject)).withCallCredentials(bearerTokenCredentials);
        } catch (Exception ex) {
            throw ex;
        }

    }

    Method getByName(String name) throws Exception {
        return getByName(abstractStub.getClass().getDeclaredMethods(), name);
    }

    Method getByName(Method[] methods, String name) throws Exception {
        Optional<Method> matchingMethod = Arrays.asList(methods).stream().filter(method -> {
            return name.equalsIgnoreCase(method.getName());
        }).findFirst();
        if (matchingMethod.isPresent()) {
            return matchingMethod.get();
        } else {
            throw new Exception("No matching method:" + name + " provided by service");
        }
    }

    public String callGrpcNoPayload(String serviceFunctionName) throws Exception {
        Method method = getByName(serviceFunctionName);
        return makeACall(serviceFunctionName, Empty.newBuilder().build(), method );
    }

    public String makeACall(String serviceFunctionName, Message message, Method method) throws Exception {
        Response response = new Response();
        ObjectMapper mapper = new ObjectMapper();
        try {
            GeneratedMessageV3 servicesResponse = (GeneratedMessageV3) method.invoke(abstractStub, message);
            String messageResponse = JsonFormat.printer().print(servicesResponse);
            JsonNode node = mapper.readTree(messageResponse);
            response.callSuccess = true;
            response.message = node;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            response.status = ((StatusRuntimeException)e.getTargetException()).getStatus().getCode().value();
            response.description = ((StatusRuntimeException)e.getTargetException()).getStatus().getDescription();
            response.callSuccess = ((StatusRuntimeException)e.getTargetException()).getStatus().isOk();
        }
        return mapper.writeValueAsString(response);
    }

    public String callGrpcWithPayload(String serviceFunctionName, String jsonPayload) throws Exception {
        if (jsonPayload != null || !jsonPayload.isEmpty()) {
            Method method = getByName(serviceFunctionName);
            AbstractMessage.Builder<?> builder = null;
            Class<?>[] types = method.getParameterTypes();
            Class<?> clazz = Class.forName(types[0].getCanonicalName());
            try {
                builder = (AbstractMessage.Builder<?>) clazz.getMethod("newBuilder").invoke(null);
                JsonFormat.parser().ignoringUnknownFields().merge(jsonPayload, builder);
            } catch (Exception e) {
                throw new Exception("ERROR:" + e.getMessage());
            }
            return makeACall(serviceFunctionName, builder.build(), method);
        }
        throw new Exception("Unexpected empty payload");
    }
}
