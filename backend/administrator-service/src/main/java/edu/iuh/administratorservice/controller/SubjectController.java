package edu.iuh.administratorservice.controller;
import edu.iuh.administratorservice.dto.ClassesCreateDTO;
import edu.iuh.administratorservice.dto.SubjectCreateDTO;
import edu.iuh.administratorservice.entity.Classes;
import edu.iuh.administratorservice.entity.Subject;
import edu.iuh.administratorservice.repository.ClassesRepository;
import edu.iuh.administratorservice.repository.SubjectRepository;
import edu.iuh.administratorservice.serialization.JsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequestMapping("/api/v1/subject")
@Controller
@Slf4j
public class SubjectController {
    private final SubjectRepository subjectRepository;
    private final JsonConverter jsonConverter;

    public SubjectController(SubjectRepository subjectRepository, JsonConverter jsonConverter) {
        this.subjectRepository = subjectRepository;
        this.jsonConverter = jsonConverter;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<String>> create(@RequestBody SubjectCreateDTO info){
        log.info("### enter api.v1.subject.create  ###");
        log.info("# info: {} #", jsonConverter.objToString(info));
        return subjectRepository.save(new Subject(info))
                .switchIfEmpty(Mono.defer(()->{
                    log.error("# {} #", "Fail save");
                    return Mono.just(ResponseEntity.status(500).body("Fail save"));
                }).then(Mono.empty()))
                .flatMap(department -> Mono.just(ResponseEntity.ok(jsonConverter.objToString(department))));
    }

    @GetMapping("/get-all")
    public Mono<ResponseEntity<String>> getAll(){
        log.info("### enter api.v1.subject.get-all ###");
        return subjectRepository.findAll(Sort.by(Sort.Order.by("name")))
                .collectList()
                .flatMap(detailCourses -> Mono.just(ResponseEntity.ok(jsonConverter.objToString(detailCourses))))
                .onErrorResume(e -> {
                    log.error("Error occurred: {}", e.getMessage());
                    return Mono.error(new Throwable(e));
                });
    }

    @GetMapping("/search-by-id")
    public Mono<ResponseEntity<String>> searchByID(@RequestParam UUID id){
        log.info("### enter api.v1.subject.search-by-id ###");
        log.info("# id: {} #", id);
        return subjectRepository.findById(id)
                .flatMap(detailCourses -> Mono.just(ResponseEntity.ok(jsonConverter.objToString(detailCourses))))
                .onErrorResume(e -> {
                    log.error("Error occurred: {}", e.getMessage());
                    return Mono.error(new Throwable(e));
                });
    }


}