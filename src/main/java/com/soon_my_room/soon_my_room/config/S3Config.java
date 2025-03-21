package com.soon_my_room.soon_my_room.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3Config {

  @Value("${supabase.endpoint}")
  private String endpoint;

  @Value("${supabase.region}")
  private String region;

  @Value("${supabase.access-key}")
  private String accessKey;

  @Value("${supabase.secret-key}")
  private String secretKey;

  @Bean
  public S3Client s3Client() {
    AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

    // 경로 스타일 엔드포인트 설정 (S3Configuration 객체 사용)
    S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build();

    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .serviceConfiguration(s3Configuration)
        .build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

    // 경로 스타일 엔드포인트 설정 (S3Configuration 객체 사용)
    S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build();

    return S3Presigner.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .serviceConfiguration(s3Configuration)
        .build();
  }
}
