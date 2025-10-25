package com.microservice.course.service;

import com.microservice.course.client.StudentClient;
import com.microservice.course.dto.StudentByCourseResponse;
import com.microservice.course.dto.StudentDTO;
import com.microservice.course.entities.Course;
import com.microservice.course.persistence.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService{

    private final CourseRepository courseRepository;
    private final StudentClient studentClient;

    @Override
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Override
    public Course findById(Long id) {
        return courseRepository.findById(id).orElseThrow();
    }

    @Override
    public Course save(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public StudentByCourseResponse findStudentsByCourseId(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow();

        List<StudentDTO> studentsList = studentClient.findAllStudentsByCourse(courseId);

        return StudentByCourseResponse.builder()
                .courseName(course.getName())
                .teacherName(course.getTeacher())
                .students(studentsList)
                .build();
    }
}
