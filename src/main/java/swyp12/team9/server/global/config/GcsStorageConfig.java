package swyp12.team9.server.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * GCP Cloud Storage 설정 클래스
 * Application Default Credentials(ADC)를 사용하여 GCP Cloud Storage에 연결
 * ADC 미설정 시 Storage Bean은 null로 생성되며, 서버는 정상 기동됨
 */
@Slf4j
@Configuration
public class GcsStorageConfig {

    @Value("${cloud.gcp.storage.project-id:#{null}}")
    private String projectId;

    @Bean
    public Storage storage() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

            StorageOptions.Builder builder = StorageOptions.newBuilder()
                    .setCredentials(credentials);

            if (projectId != null && !projectId.isBlank()) {
                builder.setProjectId(projectId);
            }

            log.info("GCP Cloud Storage 인증: Application Default Credentials 사용");
            return builder.build().getService();
        } catch (IOException e) {
            log.warn("GCP Cloud Storage 인증 불가: ADC 미설정. 파일 업로드/다운로드 기능이 비활성화됩니다.");
            return null;
        }
    }
}
