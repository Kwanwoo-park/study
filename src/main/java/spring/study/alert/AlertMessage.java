package spring.study.alert;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.Map;

@Getter
@AllArgsConstructor
public class AlertMessage {
    private String message;             // 사용자에게 전달할 메시지
    private String redirectUri;         // redirect URI
    private RequestMethod method;       // HTTP 요청 메서드
    private Map<String, Object> data;   // 화면으로 전달할 데이터

    public String showMessageAndRedirect(Model model) {
        AlertMessage param = new AlertMessage(message, redirectUri, method, data);

        model.addAttribute("params", param);

        return "alert/message";
    }
}
