package edu.gju.chatbot.gju_chatbot.common;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.gju.chatbot.gju_chatbot.exception.FileProcessingException;
import edu.gju.chatbot.gju_chatbot.exception.UnsupportedFileTypeException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(FileProcessingException.class)
  public ResponseEntity<ErrorObject> handleException(
      FileProcessingException exception) {
    return new ResponseEntity<ErrorObject>(
        new ErrorObject(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            exception.getMessage(),
            new Date()),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(UnsupportedFileTypeException.class)
  public ResponseEntity<ErrorObject> handleException(
      UnsupportedFileTypeException exception) {
    return new ResponseEntity<ErrorObject>(
        new ErrorObject(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            exception.getMessage(),
            new Date()),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }
}
