package dev.demo.cicd;
import jakarta.persistence.*;
@Entity @Table(name="tasks") public class Task {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=200) private String title;
 @Column(nullable=false) private boolean completed;
 protected Task() {}
 public Task(String title){this.title=title;}
 public Long getId(){return id;} public String getTitle(){return title;} public boolean isCompleted(){return completed;}
 public void setCompleted(boolean completed){this.completed=completed;}
}
