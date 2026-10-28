package com.microservice.student.controller;

import com.microservice.student.entities.Student;
import com.microservice.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student student) {
        Student newStudent = studentService.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(newStudent);
    }

    @GetMapping
    public ResponseEntity<List<Student>> findAllStudents() {
        List<Student> students = studentService.findAll();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> findStudentById(@PathVariable Long id) {
        Student student = studentService.findById(id);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/course/{id}")
    public ResponseEntity<List<Student>> findAllByCourseId(@PathVariable Long id) {
        List<Student> students = studentService.findByCourseId(id);
        return ResponseEntity.ok(students);
    }

}
