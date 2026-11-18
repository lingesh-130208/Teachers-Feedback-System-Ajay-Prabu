-- Create database
CREATE DATABASE IF NOT EXISTS TeachersFeedbackSystem;
USE TeachersFeedbackSystem;

-- Student table
CREATE TABLE Student (
 student_id VARCHAR(20) PRIMARY KEY,
 ame VARCHAR(100) NOT NULL,
 email VARCHAR(100) UNIQUE,
 password_hash CHAR(64) NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Faculty table
CREATE TABLE Faculty (
 faculty_id VARCHAR(50) PRIMARY KEY,
 ame VARCHAR(100) NOT NULL,
 email VARCHAR(100) UNIQUE,
 password_hash CHAR(64) NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Admin table
CREATE TABLE Admin (
 admin_id INT PRIMARY KEY AUTO_INCREMENT,
 ame VARCHAR(100) NOT NULL,
 email VARCHAR(100) UNIQUE,
 password_hash CHAR(64) NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Feedback table
CREATE TABLE Feedback (
 id INT PRIMARY KEY AUTO_INCREMENT,
 student_id VARCHAR(20) NOT NULL,
 faculty_id VARCHAR(50) NOT NULL,
 rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments TEXT,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_student_faculty (student_id, faculty_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id),
    FOREIGN KEY (faculty_id) REFERENCES Faculty(faculty_id)
);
