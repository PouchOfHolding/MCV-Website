package pt.ualg.mcv.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import lombok.experimental.StandardException;

@StandardException
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    
}
