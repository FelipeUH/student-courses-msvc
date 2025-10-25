package com.microservice.course.client;

import com.microservice.course.dto.StudentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "MSVC-STUDENT", path = "/api/students")
public interface StudentClient {

    @GetMapping("/course/{id}")
    List<StudentDTO> findAllStudentsByCourse(@PathVariable Long id);

}
