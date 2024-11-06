package vuong20194412.chat.authentication_api_gateway_service.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Component
public class SettingUtil {

    public Map<String, Object> readSetting() {
        InputStream inputStream = SettingUtil.class.getResourceAsStream("/setting.json");
        if (inputStream == null)
            throw new RuntimeException("Not found setting.json in resources");
        System.out.println(SettingUtil.class.getResource("/setting.json"));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (StreamReadException | DatabindException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println(e.getClass());
            throw new RuntimeException(e);
        }
    }

    public void writeSetting(Map<String, Object> newSetting) {
        URL settingFileUrl = SettingUtil.class.getResource("/setting.json");
        if (settingFileUrl == null)
            throw new RuntimeException("Not found setting.json in resources");

        File settingFile = new File(settingFileUrl.getFile());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(settingFile, newSetting);
        } catch (StreamReadException | DatabindException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println(e.getClass());
            throw new RuntimeException(e);
        }
    }

}
