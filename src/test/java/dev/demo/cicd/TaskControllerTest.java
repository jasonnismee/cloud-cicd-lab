package dev.demo.cicd;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TaskControllerTest {
 @Test void createsTask(){ TaskRepository repo=mock(TaskRepository.class); when(repo.save(any(Task.class))).thenAnswer(i->i.getArgument(0)); var ctl=new TaskController(repo); assertEquals("ship pipeline",ctl.create(new TaskController.NewTask(" ship pipeline ")).getTitle()); }
 @Test void missingTaskReturns404(){ TaskRepository repo=mock(TaskRepository.class); when(repo.findById(99L)).thenReturn(Optional.empty()); var ctl=new TaskController(repo); var error=assertThrows(ResponseStatusException.class,()->ctl.update(99L,new TaskController.UpdateTask(true))); assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode()); }
}
