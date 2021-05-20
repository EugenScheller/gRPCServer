package com.eugen.sandbox.jpa.h2.controller;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.lang.Math.toIntExact;

import com.eugen.sandbox.grpc.CreateTutorialServiceGrpc;
import com.eugen.sandbox.grpc.Input;
import com.eugen.sandbox.grpc.Response;
import com.eugen.sandbox.jpa.h2.model.Tutorial;
import com.eugen.sandbox.jpa.h2.repository.TutorialRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@GrpcService(interceptors = AuthenticationInterceptor.class)
public class CreateTutorialServiceImpl extends CreateTutorialServiceGrpc.CreateTutorialServiceImplBase {

    @Autowired
    TutorialRepository tutorialRepository;

    /**
     * Method to persist a tutorial
     * @param createTutorialRequest
     * @param responseObserver
     */
    @Override
    public void create(com.eugen.sandbox.grpc.Tutorial createTutorialRequest, StreamObserver<Response> responseObserver){
        Response response;
        final long startTime = System.nanoTime();
        try {
            tutorialRepository
                    .save(new Tutorial(createTutorialRequest.getTitle(), createTutorialRequest.getDescription(), createTutorialRequest.getPublished()));
            long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            response = Response.newBuilder()
                    .setResponse("OK")
                    .setCreatedTutorials(1)
                    .setNotCreatedTutorials(0)
                    .setElapsedTime(toIntExact(seconds))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }catch (Exception e){
            long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            response = Response.newBuilder()
                    .setResponse("ERROR")
                    .setCreatedTutorials(0)
                    .setNotCreatedTutorials(1)
                    .setElapsedTime(toIntExact(seconds))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    /**
     * Method to return a stream of tutorials
     * @param request
     * @param responseObserver
     */
    @Override
    public void listAllTutorials(Input request, StreamObserver<com.eugen.sandbox.grpc.Tutorial> responseObserver) {
        try {
            List<Tutorial> tutorials = new ArrayList<Tutorial>();

            if(request.getSearchString()!=null && request.getSearchString()!="")
                tutorialRepository.findByTitleContaining(request.getSearchString()).forEach(tutorials::add);
            else
                tutorialRepository.findAll().forEach(tutorials::add);

            for(Tutorial tutorial:tutorials){
                com.eugen.sandbox.grpc.Tutorial response = com.eugen.sandbox.grpc.Tutorial.newBuilder()
                        .setId(toIntExact(tutorial.getId()))
                        .setDescription(tutorial.getDescription())
                        .setPublished(tutorial.isPublished())
                        .setTitle(tutorial.getTitle())
                        .build();
                responseObserver.onNext(response);
            }
        } catch (Exception e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    /**
     * Method consumes a stream of tutorials
     * @param responseObserver
     * @return
     */
    @Override
    public StreamObserver<com.eugen.sandbox.grpc.Tutorial> createTutorials(StreamObserver<Response> responseObserver) {
        return new StreamObserver<com.eugen.sandbox.grpc.Tutorial>() {
            final long startTime = System.nanoTime();
            int createdTutorials = 0;
            int notCreatedTutorials = 0;
            @Override
            public void onNext(com.eugen.sandbox.grpc.Tutorial tutorial) {
                try {
                    tutorialRepository
                            .save(new Tutorial(tutorial.getTitle(), tutorial.getDescription(), tutorial.getPublished()));
                    createdTutorials++;
                    System.out.println(createdTutorials);
                }catch (Exception e){
                    notCreatedTutorials++;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // elaborate top notch error handling
                notCreatedTutorials++;
                System.out.println(throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
                Response response = Response.newBuilder()
                        .setResponse("OK")
                        .setCreatedTutorials(createdTutorials)
                        .setNotCreatedTutorials(notCreatedTutorials)
                        .setElapsedTime(toIntExact(seconds))
                        .build();
                responseObserver.onNext(response);
            }
        };
    }

    /**
     * bidirectional stream implementation to validate if the tutorials are already persisted
     * dont complain about useless example :)
     * @param responseObserver
     * @return
     */
    @Override
    public StreamObserver<com.eugen.sandbox.grpc.Tutorial> validateTutorials(StreamObserver<com.eugen.sandbox.grpc.Tutorial> responseObserver) {
        return new StreamObserver<com.eugen.sandbox.grpc.Tutorial>() {
            @Override
            public void onNext(com.eugen.sandbox.grpc.Tutorial tutorial) {
                List<Tutorial> persistedTutorial = tutorialRepository.findByTitleContaining(tutorial.getTitle());
                if(persistedTutorial.isEmpty()){
                    responseObserver.onNext(null);
                }
                responseObserver.onNext(tutorial);
            }

            @Override
            public void onError(Throwable throwable) {
                // elaborate top notch error handling
                System.out.println(throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
