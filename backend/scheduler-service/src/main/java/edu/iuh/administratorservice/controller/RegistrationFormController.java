package edu.iuh.administratorservice.controller;

import edu.iuh.administratorservice.dto.RegistrationFormDTO;
import edu.iuh.administratorservice.dto.RegistrationFormRemoveDTO;
import edu.iuh.administratorservice.dto.StudentAppendSubjectDTO;
import edu.iuh.administratorservice.entity.DetailCourse;
import edu.iuh.administratorservice.entity.RegistrationForm;
import edu.iuh.administratorservice.entity.Semester2;
import edu.iuh.administratorservice.entity.Subject2;
import edu.iuh.administratorservice.repository.*;
import edu.iuh.administratorservice.serialization.JsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/registration-form")
@Controller
@Slf4j
public class RegistrationFormController {
    private final RegistrationFormRepository registrationFormRepository;
    private final DetailCourseRepository detailCourseRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final WebClient.Builder builder;
    private final JsonConverter jsonConverter;

    public RegistrationFormController(RegistrationFormRepository registrationFormRepository, DetailCourseRepository detailCourseRepository, StudentRepository studentRepository, CourseRepository courseRepository, WebClient.Builder builder, JsonConverter jsonConverter) {
        this.registrationFormRepository = registrationFormRepository;
        this.detailCourseRepository = detailCourseRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.builder = builder;
        this.jsonConverter = jsonConverter;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<String>> create(ServerWebExchange exchange, @RequestBody RegistrationFormDTO info) {
        log.info("### enter api.v1.registration-form.create  ###");
        log.info("# info: {} #", jsonConverter.objToString(info));
        List<UUID> detailCourseIDs = Arrays.stream(info.getDetailCourseIDs()).toList();
        return Flux.fromIterable(detailCourseIDs)
                .flatMap(uuid -> detailCourseRepository.decreaseClassSizeAvailable(uuid)
                        .flatMap(aLong -> {
                            if(aLong<=0) return Mono.error(new RuntimeException("decrease"));
                            return Mono.just(aLong);
                        }))
                .collectList()
                .flatMap(list -> detailCourseRepository.findById(detailCourseIDs.get(0))
                        .flatMap(detailCourse -> courseRepository.findById(detailCourse.getCourseID())
                                .flatMap(course -> registrationFormRepository.save(new RegistrationForm(info.getStudentID(), course, info.getGroupNumber()))
                                        .switchIfEmpty(Mono.error(new RuntimeException("fail"))))
                        ))
                .flatMap(registrationForm -> {
                    StudentAppendSubjectDTO dto = new StudentAppendSubjectDTO(info.getStudentID(), registrationForm.getCourse().getSemester().getId(), new Subject2(registrationForm));
                    return studentRepository.findBySemesterID(dto.getId(), dto.getSemesterID())
                            .flatMap(student -> studentRepository.appendSubject(dto.getId(),dto.getSemesterID(),dto.getSubject())
                                    .flatMap(aLong -> {
                                        if(aLong<=0) return Mono.defer(()->Mono.just(ResponseEntity.status(500).body("Fail")));
                                        return Mono.just(ResponseEntity.ok("Success"));
                                    }))
                            .switchIfEmpty(Mono.defer(()->{
                                List<Subject2> subject2List = new ArrayList<>();
                                subject2List.add(dto.getSubject());
                                Semester2 semester2 = new Semester2(dto.getSemesterID(), subject2List);
                                return studentRepository.appendSubjectNotExistSemesterID(dto.getId(), semester2);
                            }).then(Mono.just(ResponseEntity.ok("Success"))));
                })
                .onErrorResume(e -> {
                    if(e.getMessage().equals("decrease"))
                        return Mono.just(ResponseEntity.status(409).body("Not available"));
                    if(e.getMessage().equals("fail")){
                        return Flux.fromIterable(detailCourseIDs)
                                .flatMap(detailCourseRepository::increaseClassSizeAvailable)
                                .then(Mono.just(ResponseEntity.status(500).body("Fail append student")));
                    }
                    return Mono.just(ResponseEntity.status(404).body("Not found"));
                });
    }

    @PostMapping("/delete")
    public Mono<ResponseEntity<String>> create(ServerWebExchange exchange, @RequestBody RegistrationFormRemoveDTO info) {
        log.info("### enter api.v1.registration-form.delete  ###");
        log.info("# info: {} #", jsonConverter.objToString(info));
        return studentRepository.removeSubject(info.getId(), info.getSemesterID(), info.getSubjectID())
                .flatMap(aLong -> {
                    if(aLong<=0) return Mono.defer(()->Mono.just(ResponseEntity.status(500).body("Fail")));
                    return registrationFormRepository.findById(info.getRegistrationFormID())
                            .flatMap(registrationForm -> detailCourseRepository.searchByCourseID(registrationForm.getCourse().getId())
                                    .collectList()
                                    .flatMap(detailCourses -> {
                                        DetailCourse detailCourse = detailCourses.get(0);
                                        DetailCourse detailCourse1 = detailCourses.get(registrationForm.getGroupNumber());
                                        List<UUID> uuids = new ArrayList<>();
                                        uuids.add(detailCourse.getId());
                                        uuids.add(detailCourse1.getId());
                                        log.info("** {} {}", uuids.get(0), uuids.get(1));
                                        return Flux.fromIterable(uuids)
                                                .flatMap(detailCourseRepository::increaseClassSizeAvailable)
                                                .flatMap(aLong1 -> {
                                                    return registrationFormRepository.deleteById(registrationForm.getId());
                                                })
                                                .then(Mono.empty());
                                    }));
                })
                .then(Mono.just(ResponseEntity.ok("Success")))
                .onErrorResume(e -> {
                    log.error("# {} #", e.getMessage());
                    return Mono.just(ResponseEntity.status(404).body("Not found"));
                });
    }



}
