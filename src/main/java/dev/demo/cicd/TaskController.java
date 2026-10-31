package dev.demo.cicd;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequestMapping("/api/tasks") public class TaskController {
 private final TaskRepository repository;
 public TaskController(TaskRepository repository){this.repository=repository;}
 public record NewTask(@NotBlank @Size(max=200) String title) {}
 public record UpdateTask(boolean completed) {}
 @GetMapping public List<Task> all(){return repository.findAll();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Task create(@Valid @RequestBody NewTask req){return repository.save(new Task(req.title().trim()));}
 @PatchMapping("/{id}") public Task update(@PathVariable Long id,@RequestBody UpdateTask req){Task task=repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));task.setCompleted(req.completed());return repository.save(task);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){if(!repository.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);repository.deleteById(id);}
}
