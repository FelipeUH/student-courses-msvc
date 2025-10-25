package com.microservice.course.controller;

import com.microservice.course.dto.StudentByCourseResponse;
import com.microservice.course.entities.Course;
import com.microservice.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<Course> createCourse(Course course) {
        Course newCourse = courseService.save(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(newCourse);
    }

    @GetMapping
    public ResponseEntity<List<Course>> findAllCourses() {
        List<Course> courses = courseService.findAll();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> findById(@PathVariable Long id) {
        Course course = courseService.findById(id);
        return ResponseEntity.ok(course);
    }

    @GetMapping("/{courseId}/students")
    public ResponseEntity<StudentByCourseResponse> findStudentsByCourseId(@PathVariable Long courseId) {
        StudentByCourseResponse students = courseService.findStudentsByCourseId(courseId);
        return ResponseEntity.ok(students);
    }
}
