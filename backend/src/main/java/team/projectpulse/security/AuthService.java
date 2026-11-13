package team.projectpulse.security;

import team.projectpulse.instructor.Instructor;
import team.projectpulse.instructor.converter.InstructorToInstructorDtoConverter;
import team.projectpulse.instructor.dto.InstructorDto;
import team.projectpulse.student.Student;
import team.projectpulse.student.converter.StudentToStudentDtoConverter;
import team.projectpulse.student.dto.StudentDto;
import team.projectpulse.user.MyUserPrincipal;
import team.projectpulse.user.PeerEvaluationUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;

    private final StudentToStudentDtoConverter studentToStudentDtoConverter;
    private final InstructorToInstructorDtoConverter instructorToInstructorDtoConverter;


    public AuthService(JwtProvider jwtProvider, StudentToStudentDtoConverter studentToStudentDtoConverter, InstructorToInstructorDtoConverter instructorToInstructorDtoConverter) {
        this.jwtProvider = jwtProvider;
        this.studentToStudentDtoConverter = studentToStudentDtoConverter;
        this.instructorToInstructorDtoConverter = instructorToInstructorDtoConverter;
    }

    public Map<String, Object> createLoginInfo(Authentication authentication) {
        // Create user info.
        if (!(authentication.getPrincipal() instanceof MyUserPrincipal principal)) {
            // E.g., a caller who presents a JWT instead of a username and password.
            throw new InsufficientAuthenticationException("Login requires username and password");
        }
        PeerEvaluationUser peerEvaluationUser = principal.getPeerEvaluationUser();

        Map<String, Object> loginResultMap = new HashMap<>();

        String roles = peerEvaluationUser.getRoles(); // Nullable: the column allows null and InstructorDto.roles is not validated.

        if ("student".equals(roles)) {
            StudentDto studentDto = this.studentToStudentDtoConverter.convert((Student) peerEvaluationUser);
            // Create a JWT.
            String token = this.jwtProvider.createToken(authentication);
            loginResultMap.put("userInfo", studentDto);
            loginResultMap.put("token", token);
        } else if (roles != null && roles.contains("instructor")) {
            InstructorDto instructorDto = this.instructorToInstructorDtoConverter.convert((Instructor) peerEvaluationUser);
            // Create a JWT.
            String token = this.jwtProvider.createToken(authentication);
            loginResultMap.put("userInfo", instructorDto);
            loginResultMap.put("token", token);
        } else {
            // No role, or a role this login does not know: refuse instead of answering success with no token.
            throw new AccessDeniedException("Login is not permitted for this account");
        }
        return loginResultMap;
    }

}
