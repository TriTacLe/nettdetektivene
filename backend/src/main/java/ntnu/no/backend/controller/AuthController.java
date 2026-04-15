package ntnu.no.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody ) {

  }

  public ResponseEntity<?> login(@Valid @RequestBody){

  }
}
