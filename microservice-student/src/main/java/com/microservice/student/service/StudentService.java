package com.microservice.student.service;

import com.microservice.student.entities.Student;

import java.util.List;

public interface StudentService {

    List<Student> findAll();

    Student findById(Long id);

    Student save(Student student);

    List<Student> findByCourseId(Long courseId);
}
