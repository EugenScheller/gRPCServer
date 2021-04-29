package com.eugen.sandbox.jpa.h2.controller;

import com.eugen.sandbox.grpc.CreateTutorialRequest;
import com.eugen.sandbox.grpc.CreateTutorialResponse;
import com.eugen.sandbox.grpc.CreateTutorialServiceGrpc;
import com.eugen.sandbox.jpa.h2.model.Tutorial;
import com.eugen.sandbox.jpa.h2.repository.TutorialRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class CreateTutorialServiceImpl extends CreateTutorialServiceGrpc.CreateTutorialServiceImplBase {

    @Autowired
    TutorialRepository tutorialRepository;

    @Override
    public void create(CreateTutorialRequest createTutorialRequest, StreamObserver<CreateTutorialResponse> responseObserver){
        CreateTutorialResponse response;
        try {
            tutorialRepository
                    .save(new Tutorial(createTutorialRequest.getTitle(), createTutorialRequest.getDescription(), createTutorialRequest.getPublished()));
            response = CreateTutorialResponse.newBuilder()
                    .setResponse(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }catch (Exception e){
            response = CreateTutorialResponse.newBuilder()
                    .setResponse(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
